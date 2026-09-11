package ru.krotarnya.diasync2.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Upsert;
import java.util.List;
import java.util.stream.Collectors;

@Dao
public interface MasterEventDao {
    record DeliveryAck(
            String eventId,
            String timestamp,
            long serverId,
            String updateTimestamp) {
    }

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

    @Query("SELECT COUNT(*) FROM master_events WHERE state != 'DELIVERED'")
    int countUndeliveredEvents();

    @Query("SELECT COUNT(*) FROM master_events WHERE state = 'BLOCKED'")
    int countBlockedEvents();

    @Query("SELECT COUNT(*) FROM master_events WHERE destination_fingerprint = :fingerprint "
            + "AND state IN ('PENDING', 'IN_FLIGHT')")
    int countRetryableEvents(String fingerprint);

    @Query("SELECT MAX(received_at) FROM master_events")
    String latestReceivedAt();

    @Query("SELECT MAX(delivered_at) FROM master_events")
    String latestDeliveredAt();

    @Query("SELECT last_error_code FROM master_events WHERE state != 'DELIVERED' "
            + "AND last_error_code IS NOT NULL ORDER BY received_at DESC LIMIT 1")
    String latestUndeliveredError();

    @Query("SELECT * FROM master_events "
            + "WHERE destination_fingerprint = :destinationFingerprint AND ("
            + "(state = 'PENDING' AND (next_attempt_at IS NULL OR next_attempt_at <= :now)) OR "
            + "(state = 'IN_FLIGHT' AND lease_until <= :now)) "
            + "ORDER BY received_at, event_id LIMIT :limit")
    List<MasterEventEntity> findEligible(
            String destinationFingerprint,
            String now,
            int limit);

    @Query("UPDATE master_events SET state = 'IN_FLIGHT', attempt_count = attempt_count + 1, "
            + "lease_until = :leaseUntil, next_attempt_at = NULL, last_error_code = NULL "
            + "WHERE event_id IN (:eventIds) AND ("
            + "state = 'PENDING' OR (state = 'IN_FLIGHT' AND lease_until <= :now))")
    int acquireLease(List<String> eventIds, String now, String leaseUntil);

    @Query("SELECT * FROM master_events WHERE event_id IN (:eventIds) "
            + "AND state = 'IN_FLIGHT' AND lease_until = :leaseUntil "
            + "ORDER BY received_at, event_id")
    List<MasterEventEntity> findLeased(List<String> eventIds, String leaseUntil);

    @Transaction
    default List<MasterEventEntity> claimEligible(
            String destinationFingerprint,
            String now,
            String leaseUntil,
            int limit
    ) {
        List<MasterEventEntity> eligible = findEligible(destinationFingerprint, now, limit);
        if (eligible.isEmpty()) {
            return List.of();
        }
        List<String> eventIds = eligible.stream()
                .map(event -> event.eventId)
                .collect(Collectors.toList());
        acquireLease(eventIds, now, leaseUntil);
        return findLeased(eventIds, leaseUntil);
    }

    @Query("UPDATE master_events SET state = 'PENDING', next_attempt_at = :nextAttemptAt, "
            + "lease_until = NULL, last_error_code = :errorCode "
            + "WHERE event_id IN (:eventIds) AND state = 'IN_FLIGHT' "
            + "AND lease_until = :leaseUntil")
    int markRetry(
            List<String> eventIds,
            String leaseUntil,
            String nextAttemptAt,
            String errorCode);

    @Query("UPDATE master_events SET state = 'BLOCKED', next_attempt_at = NULL, "
            + "lease_until = NULL, last_error_code = :errorCode "
            + "WHERE event_id IN (:eventIds) AND state = 'IN_FLIGHT' "
            + "AND lease_until = :leaseUntil")
    int markBlocked(List<String> eventIds, String leaseUntil, String errorCode);

    @Query("UPDATE master_events SET state = 'PENDING', next_attempt_at = NULL, "
            + "lease_until = NULL, last_error_code = NULL "
            + "WHERE state = 'BLOCKED' AND destination_fingerprint = :fingerprint")
    int retryBlocked(String fingerprint);

    @Query("UPDATE master_events SET state = 'DELIVERED', next_attempt_at = NULL, "
            + "lease_until = NULL, delivered_at = :deliveredAt, last_error_code = NULL "
            + "WHERE event_id = :eventId AND state = 'IN_FLIGHT' "
            + "AND lease_until = :leaseUntil")
    int markDelivered(String eventId, String leaseUntil, String deliveredAt);

    @Query("UPDATE data_points SET server_id = :serverId, update_timestamp = CASE "
            + "WHEN update_timestamp IS NULL OR update_timestamp < :updateTimestamp "
            + "THEN :updateTimestamp ELSE update_timestamp END "
            + "WHERE user_id = :userId AND timestamp = :timestamp")
    int updateServerMetadata(
            String userId,
            String timestamp,
            long serverId,
            String updateTimestamp);

    @Transaction
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean reconcileDelivered(
            String eventId,
            String leaseUntil,
            String deliveredAt,
            String userId,
            String timestamp,
            long serverId,
            String updateTimestamp
    ) {
        if (markDelivered(eventId, leaseUntil, deliveredAt) != 1) {
            return false;
        }
        if (updateServerMetadata(userId, timestamp, serverId, updateTimestamp) != 1) {
            throw new IllegalStateException("Accepted event has no local data point");
        }
        return true;
    }

    @Transaction
    default int reconcileDeliveredBatch(
            List<DeliveryAck> acknowledgements,
            String leaseUntil,
            String deliveredAt,
            String userId
    ) {
        for (DeliveryAck acknowledgement : acknowledgements) {
            if (!reconcileDelivered(
                    acknowledgement.eventId(),
                    leaseUntil,
                    deliveredAt,
                    userId,
                    acknowledgement.timestamp(),
                    acknowledgement.serverId(),
                    acknowledgement.updateTimestamp())) {
                throw new IllegalStateException("Master upload lease is no longer owned");
            }
        }
        return acknowledgements.size();
    }

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
