package ru.krotarnya.diasync2.wear;

import ru.krotarnya.diasync2.common.AlertType;

record WearVibrationCommand(long[] timings, int[] amplitudes) {
    static WearVibrationCommand forAlert(AlertType type) {
        return switch (type) {
            case LOW -> new WearVibrationCommand(
                    new long[]{800L, 400L, 800L},
                    new int[]{255, 0, 255});
            case HIGH, NO_DATA -> new WearVibrationCommand(
                    new long[]{1_000L},
                    new int[]{255});
        };
    }
}
