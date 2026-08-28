package ru.krotarnya.diasync2.settings;

public record AlertSettings(
        boolean lowEnabled,
        boolean highEnabled,
        boolean noDataEnabled
) {
    public static AlertSettings defaults() {
        return new AlertSettings(false, false, false);
    }
}
