package ru.krotarnya.diasync.common.model;

import android.content.Intent;
import android.os.BatteryManager;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BatteryStatus {
    private final int level;
    private final int scale;
    private final boolean isCharging;

    public BatteryStatus(BatteryManager batteryManager) {
        this(
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
                100,
                batteryManager.isCharging()
        );
    }

    public BatteryStatus(Intent intent) {
        this(
                intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100),
                intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING ||
                        intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_FULL
        );
    }

    public double chargeLevel() {
        return (scale > 0 && level >= 0) ? (double) level / scale : 0.0;
    }

    public int chargePercentRounded() {
        return Math.toIntExact(Math.round(chargeLevel() * 100));
    }
}
