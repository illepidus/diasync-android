package ru.krotarnya.diasync2.master;

import java.time.Duration;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

public final class MasterRetryDelay implements Function<Integer, Duration> {
    public static final Duration MAX_DELAY = Duration.ofMinutes(1);

    private final DoubleSupplier random;

    public MasterRetryDelay(DoubleSupplier random) {
        this.random = Objects.requireNonNull(random);
    }

    @Override
    public Duration apply(Integer attemptCount) {
        int exponent = Math.max(0, Math.min(attemptCount - 1, 6));
        long cappedMillis = Math.min(1_000L << exponent, MAX_DELAY.toMillis());
        double sample = random.getAsDouble();
        if (sample < 0.0 || sample >= 1.0) {
            throw new IllegalStateException("Random sample must be in [0, 1)");
        }
        return Duration.ofMillis((long) (cappedMillis * (0.5 + sample * 0.5)));
    }
}
