package ru.krotarnya.diasync.common.model;

import androidx.room.TypeConverter;

import java.time.Duration;
import java.time.Instant;

public class DateTimeConverters {
    @TypeConverter
    public static Instant instantFromLong(Long value) {
        if (value == null) {
            return null;
        }
        return Instant.ofEpochMilli(value);
    }

    @TypeConverter
    public static Long toString(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toEpochMilli();
    }

    @TypeConverter
    public static Long toLong(Duration duration) {
        if (duration == null) {
            return null;
        }
        return duration.toMillis();
    }

    @TypeConverter
    public static Duration durationFromLong(Long duration) {
        if (duration == null) {
            return null;
        }
        return Duration.ofMillis(duration);
    }
}