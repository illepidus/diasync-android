package ru.krotarnya.diasync2.navigation;

public enum PhoneScreen {
    STATUS,
    CONNECTION,
    GLUCOSE,
    WIDGET,
    WATCH,
    ALERTS,
    DIAGNOSTICS;

    public static PhoneScreen fromRoute(String route) {
        if (route == null) {
            return STATUS;
        }
        try {
            return valueOf(route);
        } catch (IllegalArgumentException exception) {
            return STATUS;
        }
    }
}
