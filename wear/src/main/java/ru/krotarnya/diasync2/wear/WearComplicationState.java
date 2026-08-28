package ru.krotarnya.diasync2.wear;

import java.time.Instant;
import java.util.List;

record WearComplicationState(
        Kind kind,
        Instant renderedAt,
        Instant latestTimestamp,
        List<GraphPoint> points,
        double latestDisplayMgDl,
        String value,
        String trend,
        String unit,
        double lowMgDl,
        double highMgDl,
        int graphWindowMinutes,
        boolean graphZones,
        boolean graphLines,
        String message,
        String contentDescription
) {
    enum Kind {
        NO_DATA,
        FRESH,
        STALE,
        FUTURE
    }

    record GraphPoint(Instant timestamp, double displayMgDl) {
    }

    WearComplicationState {
        points = List.copyOf(points);
    }

    boolean hasGlucose() {
        return kind == Kind.FRESH || kind == Kind.STALE;
    }

    String valueWithTrend() {
        return trend.isEmpty() ? value : value + " " + trend;
    }
}
