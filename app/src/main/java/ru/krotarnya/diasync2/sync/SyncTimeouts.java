package ru.krotarnya.diasync2.sync;

import java.time.Duration;

public record SyncTimeouts(Duration serverLongPoll, Duration clientRead) {
    private static final SyncTimeouts PRODUCTION = new SyncTimeouts(
            Duration.ofSeconds(75),
            Duration.ofSeconds(90));
    private static final SyncTimeouts DEBUG = new SyncTimeouts(
            Duration.ofSeconds(10),
            Duration.ofSeconds(15));

    public SyncTimeouts {
        if (serverLongPoll.isNegative()
                || serverLongPoll.isZero()
                || clientRead.compareTo(serverLongPoll) <= 0) {
            throw new IllegalArgumentException(
                    "Client read timeout must exceed a positive server timeout");
        }
    }

    public static SyncTimeouts forBuild(boolean debug) {
        return debug ? DEBUG : PRODUCTION;
    }
}
