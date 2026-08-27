package ru.krotarnya.diasync2.presentation;

public final class StatusState {
    public enum Kind {
        CONFIGURATION_MISSING,
        LOADING,
        LATEST_VALUE,
        NO_DATA,
        CONNECTION_ERROR,
        HTTP_ERROR,
        PARSE_ERROR,
        INVALID_DATA,
        STORAGE_ERROR
    }

    private final Kind kind;
    private final String value;
    private final String unit;
    private final String timestamp;
    private final String age;

    private StatusState(Kind kind, String value, String unit, String timestamp, String age) {
        this.kind = kind;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
        this.age = age;
    }

    public static StatusState simple(Kind kind) {
        return new StatusState(kind, "", "", "", "");
    }

    public static StatusState latest(String value, String unit, String timestamp, String age) {
        return new StatusState(Kind.LATEST_VALUE, value, unit, timestamp, age);
    }

    public Kind kind() {
        return kind;
    }

    public String value() {
        return value;
    }

    public String unit() {
        return unit;
    }

    public String timestamp() {
        return timestamp;
    }

    public String age() {
        return age;
    }
}
