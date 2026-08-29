package ru.krotarnya.diasync2.data;

import java.time.Instant;
import ru.krotarnya.diasync2.common.DataPoint;

public record SyncDiagnosticsData(
        DataPoint latestPoint,
        Instant lastSuccessAt,
        Instant cursorUpdateTimestamp,
        String lastError
) {}
