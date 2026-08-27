package ru.krotarnya.diasync2.settings;

import java.util.Objects;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public final class AppConfiguration {
    public static final double DEFAULT_LOW_MG_DL = 70.0;
    public static final double DEFAULT_HIGH_MG_DL = 180.0;

    private final String baseUrl;
    private final String userId;
    private final GlucoseUnit unit;
    private final boolean useCalibration;
    private final double lowMgDl;
    private final double highMgDl;
    private final GraphWindow widgetGraphWindow;
    private final boolean widgetGraphZones;
    private final boolean widgetGraphLines;
    private final boolean widgetTrendArrow;

    public AppConfiguration(
            String baseUrl,
            String userId,
            GlucoseUnit unit,
            boolean useCalibration,
            double lowMgDl,
            double highMgDl,
            GraphWindow widgetGraphWindow,
            boolean widgetGraphZones,
            boolean widgetGraphLines,
            boolean widgetTrendArrow
    ) {
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.userId = Objects.requireNonNull(userId);
        this.unit = Objects.requireNonNull(unit);
        this.useCalibration = useCalibration;
        this.lowMgDl = lowMgDl;
        this.highMgDl = highMgDl;
        this.widgetGraphWindow = Objects.requireNonNull(widgetGraphWindow);
        this.widgetGraphZones = widgetGraphZones;
        this.widgetGraphLines = widgetGraphLines;
        this.widgetTrendArrow = widgetTrendArrow;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String userId() {
        return userId;
    }

    public GlucoseUnit unit() {
        return unit;
    }

    public boolean useCalibration() {
        return useCalibration;
    }

    public double lowMgDl() {
        return lowMgDl;
    }

    public double highMgDl() {
        return highMgDl;
    }

    public GraphWindow widgetGraphWindow() {
        return widgetGraphWindow;
    }

    public boolean widgetGraphZones() {
        return widgetGraphZones;
    }

    public boolean widgetGraphLines() {
        return widgetGraphLines;
    }

    public boolean widgetTrendArrow() {
        return widgetTrendArrow;
    }
}
