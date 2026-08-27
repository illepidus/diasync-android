package ru.krotarnya.diasync2.widget;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.GlucoseValue;
import ru.krotarnya.diasync2.common.SensorPoint;
import ru.krotarnya.diasync2.common.TrendCalculator;

public final class WidgetPresenter {
    public static final double DEFAULT_LOW_MG_DL = 70.0;
    public static final double DEFAULT_HIGH_MG_DL = 180.0;

    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(1);
    private static final Duration FRESH_DURATION = Duration.ofMinutes(1);
    private static final Duration STRIKE_THROUGH_AGE = Duration.ofMinutes(10);

    private final Clock clock;
    private final TrendCalculator trendCalculator;

    public WidgetPresenter(Clock clock, TrendCalculator trendCalculator) {
        this.clock = Objects.requireNonNull(clock);
        this.trendCalculator = Objects.requireNonNull(trendCalculator);
    }

    public WidgetState present(
            List<DataPoint> points,
            GlucoseUnit unit,
            boolean useCalibration
    ) {
        Objects.requireNonNull(points);
        Objects.requireNonNull(unit);
        List<SensorPoint> sensorPoints = new ArrayList<>(points.size());
        for (DataPoint point : points) {
            if (point.sensorPoint() != null) {
                sensorPoints.add(point.sensorPoint());
            }
        }
        SensorPoint latest = sensorPoints.stream()
                .max(Comparator.comparing(SensorPoint::timestamp))
                .orElse(null);
        if (latest == null) {
            return new WidgetState("----", "", "-", "NO DATA", WidgetState.Range.ERROR, true, false);
        }

        Duration age = Duration.between(latest.timestamp(), clock.instant());
        if (age.compareTo(FUTURE_TOLERANCE.negated()) < 0) {
            return new WidgetState(
                    "",
                    "",
                    "-",
                    "DATA FROM FAR FUTURE",
                    WidgetState.Range.ERROR,
                    false,
                    false);
        }

        GlucoseValue value = latest.displayValue(useCalibration);
        String trend = trendCalculator.calculate(sensorPoints, useCalibration);
        String message = age.compareTo(FRESH_DURATION) < 0 ? "" : ageMessage(age);
        return new WidgetState(
                value.format(unit),
                unit.symbol(),
                trend.isEmpty() ? "-" : trend,
                message,
                range(value.mgDl()),
                true,
                age.compareTo(STRIKE_THROUGH_AGE) > 0);
    }

    private WidgetState.Range range(double mgDl) {
        if (mgDl <= DEFAULT_LOW_MG_DL) {
            return WidgetState.Range.LOW;
        }
        if (mgDl >= DEFAULT_HIGH_MG_DL) {
            return WidgetState.Range.HIGH;
        }
        return WidgetState.Range.NORMAL;
    }

    private String ageMessage(Duration age) {
        long minutes = age.toMinutes();
        return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
    }
}
