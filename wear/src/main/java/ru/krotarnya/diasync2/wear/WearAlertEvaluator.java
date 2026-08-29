package ru.krotarnya.diasync2.wear;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import ru.krotarnya.diasync2.common.AlertEvaluator;
import ru.krotarnya.diasync2.common.AlertType;
import ru.krotarnya.diasync2.common.wear.WearAlertEvent;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

final class WearAlertEvaluator {
    static final Duration VISUAL_STALE_AFTER = Duration.ofSeconds(90);
    static final Duration NO_DATA_AFTER = Duration.ofMinutes(5);
    private static final Duration NO_DATA_REPEAT = Duration.ofMinutes(1);

    private final Clock clock;

    WearAlertEvaluator(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    WearAlertEvaluation evaluate(WearSnapshot snapshot, WearAlertState previousState) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(previousState);
        Instant now = clock.instant();
        WearAlertPolicy policy = snapshot.alerts();
        String lastEventId = previousState.lastProcessedEventId();
        Instant lastNoDataAt = previousState.lastNoDataAlertAt();
        AlertType vibration = null;
        Instant nextCheckAt = null;

        WearAlertEvent event = snapshot.alertEvent();
        if (event != null && !event.eventId().equals(lastEventId)) {
            if (now.isBefore(event.generatedAt())) {
                nextCheckAt = earlier(nextCheckAt, event.generatedAt(), now);
            } else {
                lastEventId = event.eventId();
                if (event.shouldHandle(previousState.lastProcessedEventId(), now)
                        && enabled(policy, event.type())
                        && !policy.snoozedUntil().isAfter(now)) {
                    vibration = event.type();
                }
            }
        }

        Instant freshnessReference = snapshot.points().isEmpty()
                ? snapshot.generatedAt()
                : snapshot.points().get(0).timestamp();
        WearDataPhase phase = phase(snapshot, freshnessReference, now);
        boolean noData = !freshnessReference.plus(NO_DATA_AFTER).isAfter(now);
        if (!noData) {
            lastNoDataAt = null;
        } else if (policy.noDataEnabled()) {
            if (policy.snoozedUntil().isAfter(now)) {
                nextCheckAt = earlier(nextCheckAt, policy.snoozedUntil(), now);
            } else if (lastNoDataAt == null
                    || !lastNoDataAt.plus(AlertEvaluator.SILENCE_INTERVAL).isAfter(now)) {
                if (vibration == null) {
                    vibration = AlertType.NO_DATA;
                    lastNoDataAt = now;
                }
            }
        }

        if (snapshot.points().isEmpty() && !noData) {
            nextCheckAt = earlier(
                    nextCheckAt,
                    freshnessReference.plus(NO_DATA_AFTER),
                    now);
        } else if (phase == WearDataPhase.FRESH && !snapshot.points().isEmpty()) {
            nextCheckAt = earlier(
                    nextCheckAt,
                    freshnessReference.plus(VISUAL_STALE_AFTER).plusMillis(1),
                    now);
        } else if (phase == WearDataPhase.STALE) {
            nextCheckAt = earlier(
                    nextCheckAt,
                    freshnessReference.plus(NO_DATA_AFTER),
                    now);
        } else if (noData && policy.noDataEnabled() && !policy.snoozedUntil().isAfter(now)) {
            Instant repeatFrom = lastNoDataAt == null ? now : lastNoDataAt;
            nextCheckAt = earlier(nextCheckAt, repeatFrom.plus(NO_DATA_REPEAT), now);
        }

        WearAlertState nextState = new WearAlertState(lastEventId, lastNoDataAt, phase);
        return new WearAlertEvaluation(
                nextState,
                vibration,
                nextCheckAt,
                phase != previousState.dataPhase());
    }

    private WearDataPhase phase(WearSnapshot snapshot, Instant latest, Instant now) {
        if (snapshot.points().isEmpty() || !latest.plus(NO_DATA_AFTER).isAfter(now)) {
            return WearDataPhase.NO_DATA;
        }
        if (latest.plus(VISUAL_STALE_AFTER).isBefore(now)) {
            return WearDataPhase.STALE;
        }
        return WearDataPhase.FRESH;
    }

    private boolean enabled(WearAlertPolicy policy, AlertType type) {
        return switch (type) {
            case LOW -> policy.lowEnabled();
            case HIGH -> policy.highEnabled();
            case NO_DATA -> policy.noDataEnabled();
        };
    }

    private Instant earlier(Instant current, Instant candidate, Instant now) {
        if (!candidate.isAfter(now)) {
            return current;
        }
        return current == null || candidate.isBefore(current) ? candidate : current;
    }
}
