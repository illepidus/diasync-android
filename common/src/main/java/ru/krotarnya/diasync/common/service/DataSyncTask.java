package ru.krotarnya.diasync.common.service;

import android.util.Log;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import ru.krotarnya.diasync.common.api.DiasyncApiService;
import ru.krotarnya.diasync.common.repository.AppDatabase;
import ru.krotarnya.diasync.common.model.DataPoint;

public class DataSyncTask implements Runnable {
    private static final String TAG = DataSyncTask.class.getSimpleName();
    private static final Duration MAX_SYNC_PERIOD = Duration.ofDays(1);
    private static final Duration OVERTIME_PERIOD = Duration.ofMinutes(1);

    private final String userId;
    private final AppDatabase db;
    private final DiasyncApiService api;

    public DataSyncTask(String userId, AppDatabase db, DiasyncApiService api) {
        this.userId = userId;
        this.db = db;
        this.api = api;
    }

    @Override
    public void run() {
        try {
            runSafe();
        } catch (Exception e) {
            Log.e(TAG, "Got exception while retrieving api response", e);
        }
    }

    private void runSafe() throws Exception {
        Instant to = Instant.now().plus(OVERTIME_PERIOD);
        Instant fallbackFrom = to.minus(MAX_SYNC_PERIOD);

        Instant from = db.dataPointDao()
                .getLast(userId)
                .map(p -> p.timestamp)
                .map(t -> t.plus(Duration.ofMillis(1)))
                .map(t -> Instant.ofEpochMilli(Math.max(t.toEpochMilli(), fallbackFrom.toEpochMilli())))
                .orElse(fallbackFrom);

        List<DataPoint> response = api.getDataPoints(userId, from, to).execute().body();

        if (response == null) {
            Log.e(TAG, "Was not able to get api response");
            return;
        }

        processResponse(response);
    }

    private void processResponse(List<DataPoint> response) {
        if (response.isEmpty()) {
            Log.d(TAG, "No new data");
            return;
        }

        Log.d("SyncService", "Got " + response.size() + " points");
        db.dataPointDao().upsert(response);
    }
}
