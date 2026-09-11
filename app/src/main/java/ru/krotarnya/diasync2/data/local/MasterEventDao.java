package ru.krotarnya.diasync2.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Upsert;

@Dao
public interface MasterEventDao {
    enum Acceptance {
        ACCEPTED,
        DUPLICATE,
        HASH_CONFLICT
    }

    @Query("SELECT * FROM master_events WHERE event_id = :eventId")
    MasterEventEntity findEvent(String eventId);

    @Query("SELECT * FROM data_points WHERE user_id = :userId AND timestamp = :timestamp")
    DataPointEntity findPoint(String userId, String timestamp);

    @Upsert
    void upsertPoint(DataPointEntity point);

    @Insert
    void insertEvent(MasterEventEntity event);

    @Transaction
    default void insertPointAndEvent(DataPointEntity point, MasterEventEntity event) {
        upsertPoint(point);
        insertEvent(event);
    }

    @Transaction
    default Acceptance accept(DataPointEntity incomingPoint, MasterEventEntity incomingEvent) {
        MasterEventEntity existingEvent = findEvent(incomingEvent.eventId);
        if (existingEvent != null) {
            return existingEvent.payloadHash.equals(incomingEvent.payloadHash)
                    ? Acceptance.DUPLICATE
                    : Acceptance.HASH_CONFLICT;
        }
        DataPointEntity existingPoint = findPoint(
                incomingPoint.userId,
                incomingPoint.timestamp);
        upsertPoint(mergeNonNull(existingPoint, incomingPoint));
        insertEvent(incomingEvent);
        return Acceptance.ACCEPTED;
    }

    @Query("SELECT COUNT(*) FROM master_events")
    int countEvents();

    @Query("SELECT COUNT(*) FROM master_events WHERE state = :state")
    int countEventsInState(String state);

    private static DataPointEntity mergeNonNull(
            DataPointEntity existing,
            DataPointEntity incoming
    ) {
        if (existing == null) {
            return incoming;
        }
        return new DataPointEntity(
                incoming.userId,
                incoming.timestamp,
                incoming.timestampEpochSecond,
                incoming.timestampNano,
                incoming.serverId != null ? incoming.serverId : existing.serverId,
                incoming.updateTimestamp != null
                        ? incoming.updateTimestamp
                        : existing.updateTimestamp,
                incoming.sensorMgDl != null ? incoming.sensorMgDl : existing.sensorMgDl,
                incoming.sensorId != null ? incoming.sensorId : existing.sensorId,
                incoming.calibrationSlope != null
                        ? incoming.calibrationSlope
                        : existing.calibrationSlope,
                incoming.calibrationIntercept != null
                        ? incoming.calibrationIntercept
                        : existing.calibrationIntercept,
                incoming.manualMgDl != null ? incoming.manualMgDl : existing.manualMgDl,
                incoming.carbsGrams != null ? incoming.carbsGrams : existing.carbsGrams,
                incoming.carbsDescription != null
                        ? incoming.carbsDescription
                        : existing.carbsDescription);
    }
}
