package ru.krotarnya.diasync2.common;

public final class Calibration {
    private final double slope;
    private final double intercept;

    public Calibration(double slope, double intercept) {
        if (!Double.isFinite(slope) || !Double.isFinite(intercept)) {
            throw new IllegalArgumentException("Calibration values must be finite");
        }
        this.slope = slope;
        this.intercept = intercept;
    }

    public double slope() {
        return slope;
    }

    public double intercept() {
        return intercept;
    }

    public GlucoseValue apply(GlucoseValue rawValue) {
        return new GlucoseValue(rawValue.mgDl() * slope + intercept);
    }
}
