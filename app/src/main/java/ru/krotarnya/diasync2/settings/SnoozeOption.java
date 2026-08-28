package ru.krotarnya.diasync2.settings;

import java.time.Duration;

public enum SnoozeOption {
    FIVE_MINUTES(Duration.ofMinutes(5)),
    TEN_MINUTES(Duration.ofMinutes(10)),
    FIFTEEN_MINUTES(Duration.ofMinutes(15)),
    TWENTY_MINUTES(Duration.ofMinutes(20)),
    THIRTY_MINUTES(Duration.ofMinutes(30)),
    ONE_HOUR(Duration.ofHours(1)),
    TWO_HOURS(Duration.ofHours(2)),
    FOUR_HOURS(Duration.ofHours(4)),
    SIX_HOURS(Duration.ofHours(6)),
    EIGHT_HOURS(Duration.ofHours(8)),
    TEN_HOURS(Duration.ofHours(10)),
    TWELVE_HOURS(Duration.ofHours(12)),
    TWENTY_FOUR_HOURS(Duration.ofHours(24));

    private final Duration duration;

    SnoozeOption(Duration duration) {
        this.duration = duration;
    }

    public Duration duration() {
        return duration;
    }

    public static SnoozeOption atPosition(int position) {
        SnoozeOption[] options = values();
        return position >= 0 && position < options.length ? options[position] : FIVE_MINUTES;
    }
}
