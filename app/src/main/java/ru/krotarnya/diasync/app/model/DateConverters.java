package ru.krotarnya.diasync.app.model;

import androidx.room.TypeConverter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class DateConverters {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

    @TypeConverter
    public static Instant fromString(String value) {
        if (value == null) {
            return null;
        }
        return Instant.from(formatter.parse(value));
    }

    @TypeConverter
    public static String toString(Instant instant) {
        if (instant == null) {
            return null;
        }
        return formatter.format(instant);
    }
}