package ru.krotarnya.diasync2.common;

import java.util.Locale;

public enum GlucoseUnit {
    MG_DL("mg/dL"),
    MMOL_L("mmol/L");

    private static final double MG_DL_PER_MMOL_L = 18.0;

    private final String symbol;

    GlucoseUnit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public double fromMgDl(double mgDl) {
        return this == MG_DL ? mgDl : mgDl / MG_DL_PER_MMOL_L;
    }

    public double toMgDl(double value) {
        return this == MG_DL ? value : value * MG_DL_PER_MMOL_L;
    }

    public String formatFromMgDl(double mgDl) {
        if (this == MG_DL) {
            return String.format(Locale.ROOT, "%.0f", mgDl);
        }
        return String.format(Locale.ROOT, "%.1f", fromMgDl(mgDl));
    }
}
