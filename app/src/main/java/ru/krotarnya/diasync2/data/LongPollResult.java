package ru.krotarnya.diasync2.data;

import java.time.Instant;

public final class LongPollResult {
    public enum Kind {
        DATA,
        EMPTY,
        CONNECTION_ERROR,
        HTTP_ERROR,
        PARSE_ERROR,
        INVALID_DATA,
        STORAGE_ERROR,
        CANCELLED
    }

    private final Kind kind;
    private final Instant cursor;

    private LongPollResult(Kind kind, Instant cursor) {
        this.kind = kind;
        this.cursor = cursor;
    }

    public static LongPollResult data(Instant cursor) {
        return new LongPollResult(Kind.DATA, cursor);
    }

    public static LongPollResult of(Kind kind) {
        if (kind == Kind.DATA) {
            throw new IllegalArgumentException("Data result requires cursor");
        }
        return new LongPollResult(kind, null);
    }

    public Kind kind() {
        return kind;
    }

    public Instant cursor() {
        return cursor;
    }
}
