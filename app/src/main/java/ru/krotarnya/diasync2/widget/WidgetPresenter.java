package ru.krotarnya.diasync2.widget;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseValue;
import ru.krotarnya.diasync2.common.SensorPoint;
import ru.krotarnya.diasync2.common.TrendCalculator;
import ru.krotarnya.diasync2.settings.AppConfiguration;

public final class WidgetPresenter {
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(1);
    private static final Duration AGE_MESSAGE_DELAY = Duration.ofMinutes(2);
    private static final Duration STRIKE_THROUGH_AGE = Duration.ofMinutes(10);

    private final Clock clock;
    private final TrendCalculator trendCalculator;

    public WidgetPresenter(Clock clock, TrendCalculator trendCalculator) {
        this.clock = Objects.requireNonNull(clock);
        this.trendCalculator = Objects.requireNonNull(trendCalculator);
    }

    public WidgetState present(
            List<DataPoint> points,
            AppConfiguration configuration
    ) {
        Objects.requireNonNull(points);
        Objects.requireNonNull(configuration);
        Instant now = clock.instant();
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
            return state(
                    "----",
                    "-",
                    "NO DATA",
                    WidgetState.Range.ERROR,
                    true,
                    false,
                    now,
                    List.of(),
                    configuration,
                    false,
                    false);
        }

        Duration age = Duration.between(latest.timestamp(), now);
        if (age.compareTo(FUTURE_TOLERANCE.negated()) < 0) {
            return state(
                    "",
                    "-",
                    "DATA FROM FAR FUTURE",
                    WidgetState.Range.ERROR,
                    false,
                    false,
                    now,
                    List.of(),
                    configuration,
                    true,
                    false);
        }

        GlucoseValue value = latest.displayValue(configuration.useCalibration());
        String trend = trendCalculator.calculate(sensorPoints, configuration.useCalibration());
        String message = age.compareTo(AGE_MESSAGE_DELAY) < 0 ? "" : ageMessage(age);
        boolean noData = age.compareTo(STRIKE_THROUGH_AGE) > 0;
        List<WidgetGraphSample> samples = new ArrayList<>(sensorPoints.size());
        for (SensorPoint point : sensorPoints) {
            samples.add(new WidgetGraphSample(
                    point.timestamp(),
                    point.displayValue(configuration.useCalibration()).mgDl()));
        }
        return state(
                value.format(configuration.unit()),
                trend.isEmpty() ? "-" : trend,
                message,
                range(value.mgDl(), configuration.lowMgDl(), configuration.highMgDl()),
                true,
                noData,
                now,
                samples,
                configuration,
                !noData,
                true);
    }

    private WidgetState state(
            String value,
            String trend,
            String message,
            WidgetState.Range range,
            boolean valueVisible,
            boolean strikeThrough,
            Instant now,
            List<WidgetGraphSample> samples,
            AppConfiguration configuration,
            boolean trendVisible,
            boolean graphVisible
    ) {
        return new WidgetState(
                value,
                trend,
                message,
                range,
                valueVisible,
                strikeThrough,
                now,
                samples,
                configuration.widgetGraphWindow(),
                configuration.lowMgDl(),
                configuration.highMgDl(),
                configuration.widgetGraphZones(),
                configuration.widgetGraphLines(),
                configuration.widgetTrendArrow() && trendVisible,
                graphVisible);
    }

    private WidgetState.Range range(double mgDl, double lowMgDl, double highMgDl) {
        if (mgDl <= lowMgDl) {
            return WidgetState.Range.LOW;
        }
        if (mgDl >= highMgDl) {
            return WidgetState.Range.HIGH;
        }
        return WidgetState.Range.NORMAL;
    }

    private String ageMessage(Duration age) {
        return age.toMinutes() + "m ago";
    }
}
