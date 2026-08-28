package ru.krotarnya.diasync2.common;

public record AlertPolicy(
        boolean lowEnabled,
        boolean highEnabled,
        boolean noDataEnabled,
        double lowMgDl,
        double highMgDl
) {
    public AlertPolicy {
        if (!Double.isFinite(lowMgDl) || !Double.isFinite(highMgDl)
                || lowMgDl <= 0.0 || lowMgDl >= highMgDl) {
            throw new IllegalArgumentException("Alert thresholds are invalid");
        }
    }
}
