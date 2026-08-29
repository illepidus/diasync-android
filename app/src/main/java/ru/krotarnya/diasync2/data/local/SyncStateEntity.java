package ru.krotarnya.diasync2.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_state")
public final class SyncStateEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_id")
    public final String userId;

    @ColumnInfo(name = "cursor_update_timestamp")
    public final String cursorUpdateTimestamp;

    @ColumnInfo(name = "last_success_at")
    public final String lastSuccessAt;

    @ColumnInfo(name = "last_error")
    public final String lastError;

    @ColumnInfo(name = "source_fingerprint")
    public final String sourceFingerprint;

    @Ignore
    public SyncStateEntity(
            @NonNull String userId,
            String cursorUpdateTimestamp,
            String lastSuccessAt,
            String lastError
    ) {
        this(userId, cursorUpdateTimestamp, lastSuccessAt, lastError, null);
    }

    public SyncStateEntity(
            @NonNull String userId,
            String cursorUpdateTimestamp,
            String lastSuccessAt,
            String lastError,
            String sourceFingerprint
    ) {
        this.userId = userId;
        this.cursorUpdateTimestamp = cursorUpdateTimestamp;
        this.lastSuccessAt = lastSuccessAt;
        this.lastError = lastError;
        this.sourceFingerprint = sourceFingerprint;
    }
}
