package ru.krotarnya.diasync2.data;

import java.time.Instant;
import ru.krotarnya.diasync2.common.DataPoint;

public final class BootstrapResult {
    public enum Kind {
        SUCCESS,
        NO_DATA,
        CONNECTION_ERROR,
        HTTP_ERROR,
        PARSE_ERROR,
        INVALID_DATA,
        STORAGE_ERROR
    }

    private final Kind kind;
    private final DataPoint latestPoint;
    private final Instant initialPollSince;

    private BootstrapResult(Kind kind, DataPoint latestPoint, Instant initialPollSince) {
        this.kind = kind;
        this.latestPoint = latestPoint;
        this.initialPollSince = initialPollSince;
    }

    public static BootstrapResult success(DataPoint latestPoint, Instant initialPollSince) {
        return new BootstrapResult(Kind.SUCCESS, latestPoint, initialPollSince);
    }

    public static BootstrapResult success(DataPoint latestPoint) {
        return success(latestPoint, Instant.EPOCH);
    }

    public static BootstrapResult noData(Instant initialPollSince) {
        return new BootstrapResult(Kind.NO_DATA, null, initialPollSince);
    }

    public static BootstrapResult noData() {
        return noData(Instant.EPOCH);
    }

    public static BootstrapResult error(Kind kind) {
        if (kind == Kind.SUCCESS || kind == Kind.NO_DATA) {
            throw new IllegalArgumentException("Expected error kind");
        }
        return new BootstrapResult(kind, null, null);
    }

    public Kind kind() {
        return kind;
    }

    public DataPoint latestPoint() {
        return latestPoint;
    }

    public Instant initialPollSince() {
        return initialPollSince;
    }
}
