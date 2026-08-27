package ru.krotarnya.diasync2.presentation;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import ru.krotarnya.diasync2.common.DataAge;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.SensorPoint;
import ru.krotarnya.diasync2.data.BootstrapResult;

public final class StatusPresenter {
    private final Clock clock;

    public StatusPresenter(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public StatusState configurationMissing() {
        return StatusState.simple(StatusState.Kind.CONFIGURATION_MISSING);
    }

    public StatusState loading() {
        return StatusState.simple(StatusState.Kind.LOADING);
    }

    public StatusState local(DataPoint point, GlucoseUnit unit, boolean useCalibration) {
        return point == null
                ? StatusState.simple(StatusState.Kind.NO_DATA)
                : latest(point, unit, useCalibration);
    }

    public StatusState bootstrap(
            BootstrapResult result,
            GlucoseUnit unit,
            boolean useCalibration
    ) {
        if (result.kind() == BootstrapResult.Kind.SUCCESS) {
            return latest(result.latestPoint(), unit, useCalibration);
        }
        return StatusState.simple(switch (result.kind()) {
            case NO_DATA -> StatusState.Kind.NO_DATA;
            case CONNECTION_ERROR -> StatusState.Kind.CONNECTION_ERROR;
            case HTTP_ERROR -> StatusState.Kind.HTTP_ERROR;
            case PARSE_ERROR -> StatusState.Kind.PARSE_ERROR;
            case INVALID_DATA -> StatusState.Kind.INVALID_DATA;
            case STORAGE_ERROR -> StatusState.Kind.STORAGE_ERROR;
            case SUCCESS -> throw new IllegalStateException("Handled above");
        });
    }

    private StatusState latest(DataPoint point, GlucoseUnit unit, boolean useCalibration) {
        SensorPoint sensorPoint = Objects.requireNonNull(point.sensorPoint());
        Duration age = DataAge.at(sensorPoint.timestamp(), clock);
        String ageText;
        if (age.isNegative()) {
            ageText = "Data timestamp is in the future";
        } else if (age.toMinutes() < 1) {
            ageText = "Just now";
        } else {
            ageText = age.toMinutes() + " min ago";
        }
        return StatusState.latest(
                sensorPoint.displayValue(useCalibration).format(unit),
                unit.symbol(),
                sensorPoint.timestamp().toString(),
                ageText);
    }
}
