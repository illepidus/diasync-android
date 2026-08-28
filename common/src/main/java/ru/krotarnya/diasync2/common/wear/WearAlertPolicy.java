package ru.krotarnya.diasync2.common.wear;

import java.time.Instant;
import java.util.Objects;

public record WearAlertPolicy(
        boolean lowEnabled,
        boolean highEnabled,
        boolean noDataEnabled,
        Instant snoozedUntil
) {
    public WearAlertPolicy {
        Objects.requireNonNull(snoozedUntil);
    }
}
