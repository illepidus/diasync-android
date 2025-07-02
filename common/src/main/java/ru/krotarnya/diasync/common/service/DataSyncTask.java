package ru.krotarnya.diasync.common.service;

import android.util.Log;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import retrofit2.Response;
import ru.krotarnya.diasync.common.api.DiasyncApiService;
import ru.krotarnya.diasync.common.events.NewDataEvent;
import ru.krotarnya.diasync.common.events.NoDataEvent;
import ru.krotarnya.diasync.common.repository.DataPoint;
import ru.krotarnya.diasync.common.repository.Database;

public class DataSyncTask implements Runnable {
    private static final String TAG = DataSyncTask.class.getSimpleName();
    private static final Duration MAX_SYNC_PERIOD = Duration.ofDays(1);
    private static final Duration OVERTIME_PERIOD = Duration.ofMinutes(1);

    private final Supplier<String> userIdSupplier;
    private final Database db;
    private final DiasyncApiService api;

    public DataSyncTask(Supplier<String> userIdSupplier, Database db, DiasyncApiService api) {
        this.userIdSupplier = userIdSupplier;
        this.db = db;
        this.api = api;
    }

    @Override
    public void run() {
        String userId = userIdSupplier.get();
        try {
            runOnce(userId);
        } catch (Exception e) {
            Log.e(TAG, "Got exception while retrieving api response for " + userId + e, e);
            new NoDataEvent(userId).post();
        }
    }

    private void runOnce(String userId) throws Exception {
        Instant to = Instant.now().plus(OVERTIME_PERIOD);
        Instant fallbackFrom = to.minus(MAX_SYNC_PERIOD);

        Instant from = db.dataPointDao()
                .findLast(userId)
                .map(p -> p.timestamp)
                .map(t -> t.plus(Duration.ofMillis(1)))
                .map(t -> Instant.ofEpochMilli(Math.max(t.toEpochMilli(), fallbackFrom.toEpochMilli())))
                .orElse(fallbackFrom);

        Response<List<DataPoint>> call = api.getDataPoints(userId, from, to).execute();

        if (!call.isSuccessful() || call.body() == null) {
            Log.e(TAG, "API call failed: " + call.code());
            new NoDataEvent(userId).post();
            return;
        }

        processResponse(call.body(), userId);
    }

    private void processResponse(List<DataPoint> response, String userId) {
        if (response.isEmpty()) {
            Log.d(TAG, "No new data");
            new NoDataEvent(userId).post();
            return;
        }
        Log.d("SyncService", "Got " + response.size() + " points");
        db.dataPointDao().upsert(response);
        new NewDataEvent(response).post();
    }
}
