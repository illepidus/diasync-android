package ru.krotarnya.diasync2.settings;

import java.time.Duration;

public enum GraphWindow {
    THIRTY_MINUTES(Duration.ofMinutes(30)),
    ONE_HOUR(Duration.ofHours(1)),
    THREE_HOURS(Duration.ofHours(3));

    private final Duration duration;

    GraphWindow(Duration duration) {
        this.duration = duration;
    }

    public Duration duration() {
        return duration;
    }
}
