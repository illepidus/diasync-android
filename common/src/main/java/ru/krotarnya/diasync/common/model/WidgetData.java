package ru.krotarnya.diasync.common.model;

import java.time.Duration;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import ru.krotarnya.diasync.common.repository.DataPoint;

@Data
@Builder
public class WidgetData {
    private final List<DataPoint> dataPoints;
    private final boolean doUseCalibrations;
    private final double lowMgdl;
    private final double highMgdl;
    private final Duration timeWindow;
    private final GlucoseUnit glucoseUnit;
    private final BloodColor bloodColor;
    private final BloodColor textColor;
    private final BatteryStatus batteryStatus;
}
