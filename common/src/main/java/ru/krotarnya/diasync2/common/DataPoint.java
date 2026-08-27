package ru.krotarnya.diasync2.common;

import java.time.Instant;
import java.util.Objects;

public final class DataPoint {
    private final Instant timestamp;
    private final Instant updateTimestamp;
    private final SensorPoint sensorPoint;
    private final GlucoseValue manualGlucose;
    private final Double carbsGrams;
    private final String carbsDescription;

    public DataPoint(
            Instant timestamp,
            Instant updateTimestamp,
            SensorPoint sensorPoint,
            GlucoseValue manualGlucose,
            Double carbsGrams,
            String carbsDescription
    ) {
        this.timestamp = Objects.requireNonNull(timestamp);
        this.updateTimestamp = updateTimestamp;
        this.sensorPoint = sensorPoint;
        this.manualGlucose = manualGlucose;
        this.carbsGrams = carbsGrams;
        this.carbsDescription = carbsDescription;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public Instant updateTimestamp() {
        return updateTimestamp;
    }

    public SensorPoint sensorPoint() {
        return sensorPoint;
    }

    public GlucoseValue manualGlucose() {
        return manualGlucose;
    }

    public Double carbsGrams() {
        return carbsGrams;
    }

    public String carbsDescription() {
        return carbsDescription;
    }
}
