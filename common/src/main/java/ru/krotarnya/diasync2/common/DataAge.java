package ru.krotarnya.diasync2.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class DataAge {
    private DataAge() {
    }

    public static Duration at(Instant timestamp, Clock clock) {
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(clock);
        return Duration.between(timestamp, clock.instant());
    }
}
