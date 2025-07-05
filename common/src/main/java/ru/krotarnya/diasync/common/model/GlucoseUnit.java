package ru.krotarnya.diasync.common.model;

import java.util.function.UnaryOperator;

public enum GlucoseUnit {
    MMOL(mgdl -> mgdl / 18.018),
    MGDL(mgdl -> mgdl);

    private final UnaryOperator<Double> fromMgdlF;

    GlucoseUnit(UnaryOperator<Double> fromMgdlF) {
        this.fromMgdlF = fromMgdlF;
    }

    public double getValue(double mgdl) {
        return fromMgdlF.apply(mgdl);
    }
}
