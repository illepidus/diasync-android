package ru.krotarnya.diasync2.settings;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class SnoozeCountdown {
    private final Clock clock;

    public SnoozeCountdown(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public Optional<String> remaining(Instant snoozedUntil) {
        Objects.requireNonNull(snoozedUntil);
        long remainingMillis = Duration.between(clock.instant(), snoozedUntil).toMillis();
        if (remainingMillis <= 0L) {
            return Optional.empty();
        }
        long totalSeconds = (remainingMillis + 999L) / 1_000L;
        long hours = totalSeconds / 3_600L;
        long minutes = totalSeconds % 3_600L / 60L;
        long seconds = totalSeconds % 60L;
        return Optional.of(String.format(
                Locale.ROOT,
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds));
    }
}
