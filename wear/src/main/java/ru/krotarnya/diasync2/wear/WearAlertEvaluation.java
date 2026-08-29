package ru.krotarnya.diasync2.wear;

import java.time.Instant;
import ru.krotarnya.diasync2.common.AlertType;

record WearAlertEvaluation(
        WearAlertState state,
        AlertType vibration,
        Instant nextCheckAt,
        boolean complicationChanged
) {
}
