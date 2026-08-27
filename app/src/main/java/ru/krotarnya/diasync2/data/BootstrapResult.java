package ru.krotarnya.diasync2.data;

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

    private BootstrapResult(Kind kind, DataPoint latestPoint) {
        this.kind = kind;
        this.latestPoint = latestPoint;
    }

    public static BootstrapResult success(DataPoint latestPoint) {
        return new BootstrapResult(Kind.SUCCESS, latestPoint);
    }

    public static BootstrapResult noData() {
        return new BootstrapResult(Kind.NO_DATA, null);
    }

    public static BootstrapResult error(Kind kind) {
        if (kind == Kind.SUCCESS || kind == Kind.NO_DATA) {
            throw new IllegalArgumentException("Expected error kind");
        }
        return new BootstrapResult(kind, null);
    }

    public Kind kind() {
        return kind;
    }

    public DataPoint latestPoint() {
        return latestPoint;
    }
}
