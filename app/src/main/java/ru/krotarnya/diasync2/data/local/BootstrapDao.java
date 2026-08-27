package ru.krotarnya.diasync2.data.local;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Upsert;
import java.util.List;

@Dao
public interface BootstrapDao {
    @Upsert
    void upsertDataPoints(List<DataPointEntity> points);

    @Upsert
    void upsertSyncState(SyncStateEntity state);

    @Transaction
    default void applyBootstrap(List<DataPointEntity> points, SyncStateEntity state) {
        upsertDataPoints(points);
        upsertSyncState(state);
    }

    @Query("SELECT * FROM data_points "
            + "WHERE user_id = :userId AND sensor_mg_dl IS NOT NULL "
            + "ORDER BY timestamp_epoch_second DESC, timestamp_nano DESC LIMIT 1")
    DataPointEntity latestSensorPoint(String userId);

    @Query("SELECT * FROM data_points WHERE user_id = :userId AND timestamp = :timestamp")
    DataPointEntity find(String userId, String timestamp);

    @Query("SELECT COUNT(*) FROM data_points WHERE user_id = :userId")
    int count(String userId);

    @Query("SELECT * FROM sync_state WHERE user_id = :userId")
    SyncStateEntity syncState(String userId);
}
