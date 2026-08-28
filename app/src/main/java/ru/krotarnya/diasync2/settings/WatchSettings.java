package ru.krotarnya.diasync2.settings;

import java.util.Objects;

public record WatchSettings(
        GraphWindow graphWindow,
        boolean graphZones,
        boolean graphLines,
        boolean trendArrow
) {
    public WatchSettings {
        Objects.requireNonNull(graphWindow);
    }

    public static WatchSettings defaults() {
        return new WatchSettings(
                GraphWindow.THIRTY_MINUTES,
                true,
                false,
                true);
    }
}
