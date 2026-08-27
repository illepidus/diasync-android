package ru.krotarnya.diasync2.common;

public final class GlucoseValue {
    private final double mgDl;

    public GlucoseValue(double mgDl) {
        if (!Double.isFinite(mgDl) || mgDl < 0.0) {
            throw new IllegalArgumentException("Glucose value must be finite and non-negative");
        }
        this.mgDl = mgDl;
    }

    public double mgDl() {
        return mgDl;
    }

    public double in(GlucoseUnit unit) {
        return unit.fromMgDl(mgDl);
    }

    public String format(GlucoseUnit unit) {
        return unit.formatFromMgDl(mgDl);
    }
}
