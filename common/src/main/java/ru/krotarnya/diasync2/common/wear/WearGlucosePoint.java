package ru.krotarnya.diasync2.common.wear;

import java.time.Instant;
import java.util.Objects;

public record WearGlucosePoint(
        Instant timestamp,
        double rawMgDl,
        Double calibrationSlope,
        Double calibrationIntercept
) {
    public WearGlucosePoint {
        Objects.requireNonNull(timestamp);
        if (!Double.isFinite(rawMgDl) || rawMgDl < 0.0) {
            throw new IllegalArgumentException("Glucose value is invalid");
        }
        if ((calibrationSlope == null) != (calibrationIntercept == null)) {
            throw new IllegalArgumentException("Calibration must be complete");
        }
        if (calibrationSlope != null
                && (!Double.isFinite(calibrationSlope) || !Double.isFinite(calibrationIntercept))) {
            throw new IllegalArgumentException("Calibration values are invalid");
        }
    }

    public double displayMgDl(boolean useCalibration) {
        return useCalibration && calibrationSlope != null
                ? rawMgDl * calibrationSlope + calibrationIntercept
                : rawMgDl;
    }
}
