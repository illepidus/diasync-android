package ru.krotarnya.diasync2.widget;

import java.util.Objects;

public final class WidgetState {
    public enum Range {
        LOW,
        NORMAL,
        HIGH,
        ERROR
    }

    private final String value;
    private final String unit;
    private final String trend;
    private final String message;
    private final Range range;
    private final boolean valueVisible;
    private final boolean strikeThrough;

    public WidgetState(
            String value,
            String unit,
            String trend,
            String message,
            Range range,
            boolean valueVisible,
            boolean strikeThrough
    ) {
        this.value = Objects.requireNonNull(value);
        this.unit = Objects.requireNonNull(unit);
        this.trend = Objects.requireNonNull(trend);
        this.message = Objects.requireNonNull(message);
        this.range = Objects.requireNonNull(range);
        this.valueVisible = valueVisible;
        this.strikeThrough = strikeThrough;
    }

    public String value() {
        return value;
    }

    public String unit() {
        return unit;
    }

    public String trend() {
        return trend;
    }

    public String message() {
        return message;
    }

    public Range range() {
        return range;
    }

    public boolean valueVisible() {
        return valueVisible;
    }

    public boolean strikeThrough() {
        return strikeThrough;
    }
}
