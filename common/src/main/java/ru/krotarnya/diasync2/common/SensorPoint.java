package ru.krotarnya.diasync2.common;

import java.time.Instant;
import java.util.Objects;

public final class SensorPoint {
    private final Instant timestamp;
    private final GlucoseValue rawValue;
    private final String sensorId;
    private final Calibration calibration;

    public SensorPoint(
            Instant timestamp,
            GlucoseValue rawValue,
            String sensorId,
            Calibration calibration
    ) {
        this.timestamp = Objects.requireNonNull(timestamp);
        this.rawValue = Objects.requireNonNull(rawValue);
        this.sensorId = sensorId;
        this.calibration = calibration;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public GlucoseValue rawValue() {
        return rawValue;
    }

    public String sensorId() {
        return sensorId;
    }

    public Calibration calibration() {
        return calibration;
    }

    public GlucoseValue displayValue(boolean useCalibration) {
        return useCalibration && calibration != null ? calibration.apply(rawValue) : rawValue;
    }
}
