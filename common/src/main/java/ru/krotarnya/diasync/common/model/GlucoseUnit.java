package ru.krotarnya.diasync.common.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.UnaryOperator;

public enum GlucoseUnit {
    MGDL(mgdl -> mgdl, createFormat("0")),
    MMOL(mgdl -> mgdl / 18.018, createFormat("0.0"));

    private final UnaryOperator<Double> fromMgdlF;
    private final DecimalFormat format;

    GlucoseUnit(UnaryOperator<Double> fromMgdlF, DecimalFormat format) {
        this.fromMgdlF = fromMgdlF;
        this.format = format;
    }

    public double getValue(double mgdl) {
        return fromMgdlF.apply(mgdl);
    }

    public String format(double mgdl) {
        return format.format(getValue(mgdl));
    }

    private static DecimalFormat createFormat(String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
    }
}
