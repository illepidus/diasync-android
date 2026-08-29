package ru.krotarnya.diasync2.common.wear;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import ru.krotarnya.diasync2.common.AlertType;

public record WearAlertEvent(
        String eventId,
        AlertType type,
        Instant measurementTimestamp,
        Instant generatedAt,
        Instant expiresAt
) {
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(2);

    public WearAlertEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(measurementTimestamp);
        Objects.requireNonNull(generatedAt);
        Objects.requireNonNull(expiresAt);
        if (eventId.isBlank() || eventId.length() > 100) {
            throw new IllegalArgumentException("Alert event id is invalid");
        }
        if (type == AlertType.NO_DATA) {
            throw new IllegalArgumentException("NO DATA is evaluated on Wear");
        }
        if (!expiresAt.isAfter(generatedAt)) {
            throw new IllegalArgumentException("Alert event expiry is invalid");
        }
    }

    public static WearAlertEvent create(AlertType type, Instant measurementTimestamp) {
        return create(type, measurementTimestamp, measurementTimestamp);
    }

    public static WearAlertEvent create(
            AlertType type,
            Instant measurementTimestamp,
            Instant createdAt
    ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(measurementTimestamp);
        Objects.requireNonNull(createdAt);
        return new WearAlertEvent(
                type.name() + ":" + measurementTimestamp,
                type,
                measurementTimestamp,
                createdAt,
                createdAt.plus(DEFAULT_TTL));
    }

    public boolean shouldHandle(String lastProcessedEventId, Instant now) {
        Objects.requireNonNull(now);
        return !eventId.equals(lastProcessedEventId)
                && !now.isBefore(generatedAt)
                && now.isBefore(expiresAt);
    }
}
