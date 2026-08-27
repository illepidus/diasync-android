package ru.krotarnya.diasync2.settings;

import java.util.Objects;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public record WidgetSettings(
        GlucoseUnit unit,
        boolean useCalibration,
        double lowMgDl,
        double highMgDl,
        GraphWindow graphWindow,
        boolean graphZones,
        boolean graphLines,
        boolean trendArrow
) {
    public WidgetSettings {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(graphWindow);
        if (!Double.isFinite(lowMgDl) || !Double.isFinite(highMgDl)
                || lowMgDl <= 0.0 || lowMgDl >= highMgDl) {
            throw new IllegalArgumentException("Widget thresholds are invalid");
        }
    }

    public static WidgetSettings defaults() {
        return new WidgetSettings(
                GlucoseUnit.MMOL_L,
                true,
                AppConfiguration.DEFAULT_LOW_MG_DL,
                AppConfiguration.DEFAULT_HIGH_MG_DL,
                GraphWindow.THIRTY_MINUTES,
                true,
                false,
                true);
    }
}
