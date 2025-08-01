package ru.krotarnya.diasync.common.events;

import lombok.Data;
import ru.krotarnya.diasync.common.model.BatteryStatus;

@Data
public class BatteryStatusChanged implements Event<BatteryStatus> {
    private final BatteryStatus batteryStatus;
}
