package ru.krotarnya.diasync2.master;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import ru.krotarnya.diasync2.common.master.XdripEvent;
import ru.krotarnya.diasync2.data.local.DataPointEntity;
import ru.krotarnya.diasync2.data.local.MasterEventDao;
import ru.krotarnya.diasync2.data.local.MasterEventEntity;
import ru.krotarnya.diasync2.settings.AppConfiguration;

public final class MasterEventRepository {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final MasterEventDao dao;
    private final Clock clock;

    public MasterEventRepository(MasterEventDao dao, Clock clock) {
        this.dao = Objects.requireNonNull(dao);
        this.clock = Objects.requireNonNull(clock);
    }

    public MasterEventDao.Acceptance accept(
            XdripEvent event,
            byte[] canonicalPayload,
            AppConfiguration configuration
    ) {
        Objects.requireNonNull(event);
        Objects.requireNonNull(canonicalPayload);
        Objects.requireNonNull(configuration);
        Instant occurredAt = Instant.ofEpochMilli(event.occurredAtEpochMillis());
        String canonicalJson = new String(canonicalPayload, StandardCharsets.UTF_8);
        MasterEventEntity outboxEvent = new MasterEventEntity(
                event.eventId(),
                sha256(canonicalPayload),
                event.eventType().name(),
                occurredAt.toString(),
                destinationFingerprint(configuration),
                canonicalJson,
                MasterEventState.PENDING.name(),
                0,
                null,
                null,
                clock.instant().toString(),
                null,
                null);
        return dao.accept(toPoint(event, configuration.userId(), occurredAt), outboxEvent);
    }

    static String destinationFingerprint(AppConfiguration configuration) {
        return sha256((configuration.baseUrl() + "\u0000" + configuration.userId())
                .getBytes(StandardCharsets.UTF_8));
    }

    private static DataPointEntity toPoint(XdripEvent event, String userId, Instant occurredAt) {
        Double sensorMgDl = event.sensor() == null ? null : event.sensor().rawValue();
        String sensorId = event.sensor() == null ? null : event.sensor().sensorId();
        Double slope = event.sensor() == null ? null : event.sensor().calibration().slope();
        Double intercept = event.sensor() == null
                ? null
                : event.sensor().calibration().intercept();
        Double manualMgDl = event.manualGlucose() == null
                ? null
                : event.manualGlucose().mgdl();
        Double carbsGrams = event.carbs() == null ? null : event.carbs().grams();
        String carbsDescription = event.carbs() == null ? null : event.carbs().description();
        return new DataPointEntity(
                userId,
                occurredAt.toString(),
                occurredAt.getEpochSecond(),
                occurredAt.getNano(),
                null,
                null,
                sensorMgDl,
                sensorId,
                slope,
                intercept,
                manualMgDl,
                carbsGrams,
                carbsDescription);
    }

    private static String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                int unsigned = item & 0xff;
                result.append(HEX[unsigned >>> 4]);
                result.append(HEX[unsigned & 0x0f]);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
