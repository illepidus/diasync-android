package ru.krotarnya.diasync2.wear;

import java.time.Instant;
import java.util.Objects;

record WearAlertState(
        String lastProcessedEventId,
        Instant lastNoDataAlertAt,
        WearDataPhase dataPhase
) {
    WearAlertState {
        Objects.requireNonNull(dataPhase);
    }

    static WearAlertState empty() {
        return new WearAlertState(null, null, WearDataPhase.FRESH);
    }
}
