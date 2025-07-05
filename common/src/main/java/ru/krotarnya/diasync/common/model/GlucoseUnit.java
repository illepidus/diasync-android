package ru.krotarnya.diasync.common.model;

import java.util.function.UnaryOperator;

public enum GlucoseUnit {
    MGDL(mgdl -> mgdl, "%.0f"),
    MMOL(mgdl -> mgdl / 18.018, "%.1f"),
    ;

    private final UnaryOperator<Double> fromMgdlF;
    private final String format;

    GlucoseUnit(UnaryOperator<Double> fromMgdlF, String format) {
        this.fromMgdlF = fromMgdlF;
        this.format = format;
    }

    public double getValue(double mgdl) {
        return fromMgdlF.apply(mgdl);
    }

    public String format(double mgdl) {
        return String.format(format, getValue(mgdl));
    }
}
