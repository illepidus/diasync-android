package ru.krotarnya.diasync2.alert;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import ru.krotarnya.diasync2.common.AlertDecision;
import ru.krotarnya.diasync2.common.AlertEvaluator;
import ru.krotarnya.diasync2.common.AlertPolicy;
import ru.krotarnya.diasync2.common.AlertReading;
import ru.krotarnya.diasync2.common.AlertType;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.settings.AlertSettings;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.settings.WidgetSettings;

public final class PhoneAlertController {
    @FunctionalInterface
    public interface DataSource {
        List<DataPoint> latestSensorPoints(String userId, int limit);
    }

    private final AppPreferences preferences;
    private final DataSource dataSource;
    private final AlertEvaluator evaluator;
    private final Consumer<AlertType> soundPlayer;
    private final Consumer<AlertType> notificationPublisher;
    private final AlertEventOutput eventOutput;
    private final Executor executor;

    public PhoneAlertController(
            AppPreferences preferences,
            DataSource dataSource,
            AlertEvaluator evaluator,
            Consumer<AlertType> soundPlayer,
            Consumer<AlertType> notificationPublisher,
            AlertEventOutput eventOutput,
            Executor executor
    ) {
        this.preferences = Objects.requireNonNull(preferences);
        this.dataSource = Objects.requireNonNull(dataSource);
        this.evaluator = Objects.requireNonNull(evaluator);
        this.soundPlayer = Objects.requireNonNull(soundPlayer);
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher);
        this.eventOutput = Objects.requireNonNull(eventOutput);
        this.executor = Objects.requireNonNull(executor);
    }

    public void checkAsync() {
        executor.execute(this::checkNow);
    }

    void checkNow() {
        Optional<AppConfiguration> configuration = preferences.load();
        if (configuration.isEmpty()) {
            return;
        }
        AlertSettings settings = preferences.loadAlertSettings();
        if (!settings.lowEnabled() && !settings.highEnabled() && !settings.noDataEnabled()) {
            return;
        }
        WidgetSettings widgetSettings = preferences.loadWidgetSettings();
        List<DataPoint> points = dataSource.latestSensorPoints(
                configuration.get().userId(),
                2);
        AlertReading latest = points.isEmpty()
                ? null
                : reading(points.get(0), widgetSettings.useCalibration());
        AlertReading previous = points.size() < 2
                ? null
                : reading(points.get(1), widgetSettings.useCalibration());
        AlertPolicy policy = new AlertPolicy(
                settings.lowEnabled(),
                settings.highEnabled(),
                settings.noDataEnabled(),
                widgetSettings.lowMgDl(),
                widgetSettings.highMgDl());
        AlertDecision decision = evaluator.evaluate(
                latest,
                previous,
                policy,
                preferences.snoozedUntil());
        if (!decision.shouldAlert()) {
            return;
        }
        AlertType type = decision.type().orElseThrow();
        preferences.saveLastAlertAt(evaluator.lastAlertAt());
        soundPlayer.accept(type);
        notificationPublisher.accept(type);
        if (type != AlertType.NO_DATA && latest != null) {
            eventOutput.onGlucoseAlert(type, latest.timestamp());
        }
    }

    private AlertReading reading(DataPoint point, boolean useCalibration) {
        return new AlertReading(
                point.sensorPoint().timestamp(),
                point.sensorPoint().displayValue(useCalibration).mgDl());
    }
}
