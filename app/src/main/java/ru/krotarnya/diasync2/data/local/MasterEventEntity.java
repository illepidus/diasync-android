package ru.krotarnya.diasync2.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "master_events",
        indices = {
                @Index(value = {"state", "next_attempt_at", "received_at"}),
                @Index(value = {"delivered_at"})
        })
public final class MasterEventEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "event_id")
    public final String eventId;

    @NonNull
    @ColumnInfo(name = "payload_hash")
    public final String payloadHash;

    @NonNull
    @ColumnInfo(name = "event_type")
    public final String eventType;

    @NonNull
    @ColumnInfo(name = "occurred_at")
    public final String occurredAt;

    @NonNull
    @ColumnInfo(name = "destination_fingerprint")
    public final String destinationFingerprint;

    @NonNull
    @ColumnInfo(name = "payload_json")
    public final String payloadJson;

    @NonNull
    public final String state;

    @ColumnInfo(name = "attempt_count")
    public final int attemptCount;

    @ColumnInfo(name = "next_attempt_at")
    public final String nextAttemptAt;

    @ColumnInfo(name = "lease_until")
    public final String leaseUntil;

    @NonNull
    @ColumnInfo(name = "received_at")
    public final String receivedAt;

    @ColumnInfo(name = "delivered_at")
    public final String deliveredAt;

    @ColumnInfo(name = "last_error_code")
    public final String lastErrorCode;

    public MasterEventEntity(
            @NonNull String eventId,
            @NonNull String payloadHash,
            @NonNull String eventType,
            @NonNull String occurredAt,
            @NonNull String destinationFingerprint,
            @NonNull String payloadJson,
            @NonNull String state,
            int attemptCount,
            String nextAttemptAt,
            String leaseUntil,
            @NonNull String receivedAt,
            String deliveredAt,
            String lastErrorCode
    ) {
        this.eventId = eventId;
        this.payloadHash = payloadHash;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.destinationFingerprint = destinationFingerprint;
        this.payloadJson = payloadJson;
        this.state = state;
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.leaseUntil = leaseUntil;
        this.receivedAt = receivedAt;
        this.deliveredAt = deliveredAt;
        this.lastErrorCode = lastErrorCode;
    }
}
