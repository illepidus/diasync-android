package ru.krotarnya.diasync2.widget;

import java.time.Instant;
import java.util.Objects;

public record WidgetGraphSample(Instant timestamp, double mgDl) {
    public WidgetGraphSample {
        Objects.requireNonNull(timestamp);
        if (!Double.isFinite(mgDl)) {
            throw new IllegalArgumentException("Glucose value must be finite");
        }
    }
}
