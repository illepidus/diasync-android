package ru.krotarnya.diasync2.common.wear;

import java.util.Objects;
import java.util.Set;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public record WearDisplayPolicy(
        GlucoseUnit unit,
        boolean useCalibration,
        double lowMgDl,
        double highMgDl,
        int graphWindowMinutes,
        boolean graphZones,
        boolean graphLines,
        boolean trendArrow,
        String trend
) {
    private static final Set<Integer> SUPPORTED_GRAPH_WINDOWS = Set.of(30, 60, 180);
    private static final Set<String> SUPPORTED_TRENDS = Set.of("", "⇊", "↓", "↘", "→", "↗", "↑", "⇈");

    public WearDisplayPolicy {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(trend);
        if (!Double.isFinite(lowMgDl) || !Double.isFinite(highMgDl)
                || lowMgDl <= 0.0 || lowMgDl >= highMgDl) {
            throw new IllegalArgumentException("Display thresholds are invalid");
        }
        if (!SUPPORTED_GRAPH_WINDOWS.contains(graphWindowMinutes)) {
            throw new IllegalArgumentException("Graph window is unsupported");
        }
        if (!SUPPORTED_TRENDS.contains(trend)) {
            throw new IllegalArgumentException("Trend is unsupported");
        }
    }
}
