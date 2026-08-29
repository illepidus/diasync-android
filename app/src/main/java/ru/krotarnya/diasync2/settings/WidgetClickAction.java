package ru.krotarnya.diasync2.settings;

public enum WidgetClickAction {
    DIASYNC,
    ALERTS,
    XDRIP,
    NONE;

    public static WidgetClickAction atPosition(int position) {
        WidgetClickAction[] values = values();
        return position >= 0 && position < values.length ? values[position] : DIASYNC;
    }
}
