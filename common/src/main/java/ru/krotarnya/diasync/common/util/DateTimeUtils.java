package ru.krotarnya.diasync.common.util;

import java.time.ZonedDateTime;

public class DateTimeUtils {
    public static ZonedDateTime toStartOfNMinutes(ZonedDateTime dateTime, int n) {
        int minute = Math.abs(n) % 60;

        return dateTime
                .withMinute(minute == 0 ? 0 : dateTime.getMinute() / minute * minute)
                .withSecond(0)
                .withNano(0);
    }

    public static ZonedDateTime toStartOfHour(ZonedDateTime dateTime) {
        return toStartOfNMinutes(dateTime, 0);
    }
}
