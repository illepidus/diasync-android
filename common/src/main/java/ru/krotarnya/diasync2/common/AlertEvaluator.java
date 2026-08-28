package ru.krotarnya.diasync2.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class AlertEvaluator {
    public static final Duration SILENCE_INTERVAL = Duration.ofSeconds(55);
    public static final Duration NO_DATA_INTERVAL = Duration.ofMinutes(5);

    private final Clock clock;
    private Instant lastAlertAt;

    public AlertEvaluator(Clock clock) {
        this(clock, Instant.EPOCH);
    }

    public AlertEvaluator(Clock clock, Instant lastAlertAt) {
        this.clock = Objects.requireNonNull(clock);
        this.lastAlertAt = Objects.requireNonNull(lastAlertAt);
    }

    public synchronized AlertDecision evaluate(
            AlertReading latest,
            AlertReading previous,
            AlertPolicy policy,
            Instant snoozedUntil
    ) {
        Objects.requireNonNull(policy);
        Objects.requireNonNull(snoozedUntil);
        Instant now = clock.instant();
        if (snoozedUntil.isAfter(now)) {
            return AlertDecision.none();
        }

        AlertType candidate = candidate(latest, previous, policy, now);
        if (candidate == null || lastAlertAt.plus(SILENCE_INTERVAL).isAfter(now)) {
            return AlertDecision.none();
        }
        lastAlertAt = now;
        return AlertDecision.alert(candidate);
    }

    public synchronized Instant lastAlertAt() {
        return lastAlertAt;
    }

    private AlertType candidate(
            AlertReading latest,
            AlertReading previous,
            AlertPolicy policy,
            Instant now
    ) {
        if (latest != null
                && policy.lowEnabled()
                && latest.mgDl() <= policy.lowMgDl()
                && (previous == null || latest.mgDl() < previous.mgDl())) {
            return AlertType.LOW;
        }
        if (latest != null
                && policy.highEnabled()
                && latest.mgDl() >= policy.highMgDl()
                && (previous == null || latest.mgDl() > previous.mgDl())) {
            return AlertType.HIGH;
        }
        if (policy.noDataEnabled()
                && (latest == null
                || !latest.timestamp().plus(NO_DATA_INTERVAL).isAfter(now))) {
            return AlertType.NO_DATA;
        }
        return null;
    }
}
