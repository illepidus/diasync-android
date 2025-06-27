package ru.krotarnya.diasync.common.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import retrofit2.Response;
import ru.krotarnya.diasync.common.api.DiasyncApiService;
import ru.krotarnya.diasync.common.intent.NewDataIntent;
import ru.krotarnya.diasync.common.intent.NoDataIntent;
import ru.krotarnya.diasync.common.model.DataPoint;
import ru.krotarnya.diasync.common.repository.DiasyncDatabase;

public class DataSyncTask implements Runnable {
    private static final String TAG = DataSyncTask.class.getSimpleName();
    private static final Duration MAX_SYNC_PERIOD = Duration.ofDays(1);
    private static final Duration OVERTIME_PERIOD = Duration.ofMinutes(1);

    private final Supplier<String> userIdSupplier;
    private final DiasyncDatabase db;
    private final DiasyncApiService api;
    private final Context context;

    public DataSyncTask(Supplier<String> userIdSupplier, DiasyncDatabase db, DiasyncApiService api, Context context) {
        this.userIdSupplier = userIdSupplier;
        this.db = db;
        this.api = api;
        this.context = context;
    }

    @Override
    public void run() {
        String userId = userIdSupplier.get();
        try {
            runOnce(userId);
        } catch (Exception e) {
            Log.e(TAG, "Got exception while retrieving api response for " + userId, e);
            broadcast(NoDataIntent.build(userId));
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
            broadcast(NoDataIntent.build(userId));
            return;
        }

        processResponse(call.body(), userId);
    }

    private void processResponse(List<DataPoint> response, String userId) {
        if (response.isEmpty()) {
            Log.d(TAG, "No new data");
            broadcast(NoDataIntent.build(userId));
            return;
        }

        Log.d("SyncService", "Got " + response.size() + " points");
        db.dataPointDao().upsert(response);
        broadcast(NewDataIntent.build(userId));
    }

    private void broadcast(Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
