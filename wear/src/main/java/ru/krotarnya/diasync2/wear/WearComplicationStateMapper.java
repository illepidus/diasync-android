package ru.krotarnya.diasync2.wear;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import ru.krotarnya.diasync2.common.GlucoseValue;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

final class WearComplicationStateMapper {
    static final Duration STALE_AFTER = Duration.ofSeconds(90);
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(1);

    private final Clock clock;

    WearComplicationStateMapper() {
        this(Clock.systemUTC());
    }

    WearComplicationStateMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    WearComplicationState map(Optional<WearSnapshot> snapshot) {
        Instant now = clock.instant();
        if (snapshot.isEmpty() || snapshot.orElseThrow().points().isEmpty()) {
            return error(WearComplicationState.Kind.NO_DATA, now, "NO DATA", "No glucose data");
        }
        WearSnapshot source = snapshot.orElseThrow();
        WearDisplayPolicy display = source.display();
        WearGlucosePoint latest = source.points().get(0);
        double latestMgDl = latest.displayMgDl(display.useCalibration());
        if (!Double.isFinite(latestMgDl) || latestMgDl < 0.0) {
            return error(WearComplicationState.Kind.NO_DATA, now, "NO DATA", "No glucose data");
        }
        if (latest.timestamp().isAfter(now.plus(FUTURE_TOLERANCE))) {
            return error(
                    WearComplicationState.Kind.FUTURE,
                    now,
                    "FUTURE DATA",
                    "Glucose data is from the future");
        }

        Instant windowStart = now.minus(Duration.ofMinutes(display.graphWindowMinutes()));
        List<WearComplicationState.GraphPoint> points = new ArrayList<>();
        for (WearGlucosePoint point : source.points()) {
            double displayMgDl = point.displayMgDl(display.useCalibration());
            if (!point.timestamp().isBefore(windowStart)
                    && !point.timestamp().isAfter(now.plus(FUTURE_TOLERANCE))
                    && Double.isFinite(displayMgDl)
                    && displayMgDl >= 0.0) {
                points.add(new WearComplicationState.GraphPoint(point.timestamp(), displayMgDl));
            }
        }

        Duration age = Duration.between(latest.timestamp(), now);
        if (age.compareTo(WearAlertEvaluator.NO_DATA_AFTER) >= 0) {
            return error(WearComplicationState.Kind.NO_DATA, now, "NO DATA", "No glucose data");
        }
        boolean stale = age.compareTo(STALE_AFTER) > 0;
        String value = new GlucoseValue(latestMgDl).format(display.unit());
        String trend = display.trendArrow() ? display.trend() : "";
        String message = stale ? Math.max(0L, age.toMinutes()) + "m" : "";
        String description = "Glucose " + value + " " + display.unit().symbol()
                + (trend.isEmpty() ? "" : ", trend " + trend)
                + (stale ? ", " + message + " old" : "");
        return new WearComplicationState(
                stale ? WearComplicationState.Kind.STALE : WearComplicationState.Kind.FRESH,
                now,
                latest.timestamp(),
                points,
                latestMgDl,
                value,
                trend,
                display.unit().symbol(),
                display.lowMgDl(),
                display.highMgDl(),
                display.graphWindowMinutes(),
                display.graphZones(),
                display.graphLines(),
                message,
                description);
    }

    private WearComplicationState error(
            WearComplicationState.Kind kind,
            Instant now,
            String message,
            String description
    ) {
        return new WearComplicationState(
                kind,
                now,
                null,
                List.of(),
                Double.NaN,
                "",
                "",
                "",
                70.0,
                180.0,
                30,
                false,
                false,
                message,
                description);
    }
}
