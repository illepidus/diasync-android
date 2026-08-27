package ru.krotarnya.diasync2.sync;

import java.time.Duration;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class BackoffPolicy {
    public static final Duration MAX_DELAY = Duration.ofSeconds(60);
    private static final long BASE_DELAY_MILLIS = 1_000L;

    private final DoubleSupplier random;
    private int failures;

    public BackoffPolicy(DoubleSupplier random) {
        this.random = Objects.requireNonNull(random);
    }

    public Duration nextDelay() {
        long exponential = BASE_DELAY_MILLIS << Math.min(failures, 6);
        long capped = Math.min(exponential, MAX_DELAY.toMillis());
        failures++;
        double sample = random.getAsDouble();
        if (sample < 0.0 || sample >= 1.0) {
            throw new IllegalStateException("Random sample must be in [0, 1)");
        }
        return Duration.ofMillis((long) (capped * (0.5 + sample * 0.5)));
    }

    public void reset() {
        failures = 0;
    }
}
