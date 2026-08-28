package ru.krotarnya.diasync2.alert;

import java.time.Instant;
import ru.krotarnya.diasync2.common.AlertType;

@FunctionalInterface
public interface AlertEventOutput {
    AlertEventOutput NONE = (type, measurementTimestamp) -> { };

    void onGlucoseAlert(AlertType type, Instant measurementTimestamp);
}
