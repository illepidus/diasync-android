package ru.krotarnya.diasync2.wear;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import ru.krotarnya.diasync2.common.Calibration;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.SensorPoint;
import ru.krotarnya.diasync2.common.TrendCalculator;
import ru.krotarnya.diasync2.common.wear.WearAlertEvent;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;
import ru.krotarnya.diasync2.settings.AlertSettings;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.WatchSettings;

public final class WearSnapshotBuilder {
    @FunctionalInterface
    public interface DataSource {
        List<DataPoint> latestSensorPoints(String userId, Instant from, int limit);
    }

    private final DataSource dataSource;
    private final TrendCalculator trendCalculator;
    private final Clock clock;

    public WearSnapshotBuilder(
            DataSource dataSource,
            TrendCalculator trendCalculator,
            Clock clock
    ) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.trendCalculator = Objects.requireNonNull(trendCalculator);
        this.clock = Objects.requireNonNull(clock);
    }

    public WearSnapshot build(
            AppConfiguration configuration,
            WatchSettings watchSettings,
            AlertSettings alertSettings,
            Instant snoozedUntil,
            WearAlertEvent alertEvent
    ) {
        Instant generatedAt = clock.instant();
        Instant oldest = generatedAt.minus(WearSnapshot.MAX_GRAPH_WINDOW)
                .minus(WearSnapshot.GRAPH_WINDOW_MARGIN);
        List<DataPoint> domainPoints = dataSource.latestSensorPoints(
                configuration.userId(),
                oldest,
                WearSnapshot.MAX_POINTS);
        List<DataPoint> boundedPoints = domainPoints.stream()
                .filter(point -> !point.timestamp().isBefore(oldest))
                .filter(point -> !point.timestamp().isAfter(
                        generatedAt.plus(WearSnapshot.MAX_FUTURE_SKEW)))
                .sorted(Comparator.comparing(DataPoint::timestamp).reversed())
                .limit(WearSnapshot.MAX_POINTS)
                .collect(Collectors.toList());
        List<SensorPoint> sensorPoints = boundedPoints.stream()
                .map(DataPoint::sensorPoint)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<WearGlucosePoint> wirePoints = new ArrayList<>(sensorPoints.size());
        for (SensorPoint point : sensorPoints) {
            Calibration calibration = point.calibration();
            wirePoints.add(new WearGlucosePoint(
                    point.timestamp(),
                    point.rawValue().mgDl(),
                    calibration == null ? null : calibration.slope(),
                    calibration == null ? null : calibration.intercept()));
        }
        WearDisplayPolicy display = new WearDisplayPolicy(
                configuration.unit(),
                configuration.useCalibration(),
                configuration.lowMgDl(),
                configuration.highMgDl(),
                Math.toIntExact(watchSettings.graphWindow().duration().toMinutes()),
                watchSettings.graphZones(),
                watchSettings.graphLines(),
                watchSettings.trendArrow(),
                trendCalculator.calculate(sensorPoints, configuration.useCalibration()));
        WearAlertPolicy alerts = new WearAlertPolicy(
                alertSettings.lowEnabled(),
                alertSettings.highEnabled(),
                alertSettings.noDataEnabled(),
                snoozedUntil);
        return new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                generatedAt,
                wirePoints,
                display,
                alerts,
                alertEvent);
    }
}
