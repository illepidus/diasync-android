package ru.krotarnya.diasync2.common;

import java.time.Instant;
import java.util.Objects;

public record AlertReading(Instant timestamp, double mgDl) {
    public AlertReading {
        Objects.requireNonNull(timestamp);
        if (!Double.isFinite(mgDl) || mgDl < 0.0) {
            throw new IllegalArgumentException("Alert glucose value is invalid");
        }
    }
}
