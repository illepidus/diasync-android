package ru.krotarnya.diasync2.settings;

import java.util.Locale;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public final class ThresholdDisplay {
    public static final double MIN_LOW_MMOL_L = 2.0;
    public static final double MAX_HIGH_MMOL_L = 30.0;

    public String format(double mgDl, GlucoseUnit unit) {
        return unit == GlucoseUnit.MMOL_L
                ? String.format(Locale.ROOT, "%.1f", unit.fromMgDl(mgDl))
                : String.format(Locale.ROOT, "%.0f", mgDl);
    }

    public double parseMgDl(String input, GlucoseUnit unit, String name) {
        try {
            double displayValue = normalize(Double.parseDouble(input.trim()), unit);
            double mgDl = unit.toMgDl(displayValue);
            if (!Double.isFinite(mgDl) || mgDl <= 0.0) {
                throw new NumberFormatException();
            }
            return mgDl;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    name + " must be a positive " + unit.symbol() + " number");
        }
    }

    public void validateRange(double lowMgDl, double highMgDl, GlucoseUnit unit) {
        double minimumLowMgDl = GlucoseUnit.MMOL_L.toMgDl(MIN_LOW_MMOL_L);
        double maximumHighMgDl = GlucoseUnit.MMOL_L.toMgDl(MAX_HIGH_MMOL_L);
        if (lowMgDl < minimumLowMgDl) {
            throw new IllegalArgumentException(unit == GlucoseUnit.MMOL_L
                    ? "Low threshold must be at least 2.0 mmol/L"
                    : "Low threshold must be at least 36 mg/dL");
        }
        if (highMgDl > maximumHighMgDl) {
            throw new IllegalArgumentException(unit == GlucoseUnit.MMOL_L
                    ? "High threshold must be at most 30.0 mmol/L"
                    : "High threshold must be at most 540 mg/dL");
        }
        if (lowMgDl >= highMgDl) {
            throw new IllegalArgumentException("Low threshold must be below high threshold");
        }
    }

    private double normalize(double displayValue, GlucoseUnit unit) {
        return unit == GlucoseUnit.MMOL_L
                ? Math.round(displayValue * 10.0) / 10.0
                : Math.round(displayValue);
    }
}
