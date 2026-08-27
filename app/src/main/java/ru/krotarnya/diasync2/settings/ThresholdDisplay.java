package ru.krotarnya.diasync2.settings;

import java.util.Locale;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public final class ThresholdDisplay {
    public String format(double mgDl, GlucoseUnit unit) {
        return unit == GlucoseUnit.MMOL_L
                ? String.format(Locale.ROOT, "%.1f", unit.fromMgDl(mgDl))
                : String.format(Locale.ROOT, "%.0f", mgDl);
    }

    public double parseMgDl(String input, GlucoseUnit unit, String name) {
        try {
            double displayValue = Double.parseDouble(input.trim());
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
}
