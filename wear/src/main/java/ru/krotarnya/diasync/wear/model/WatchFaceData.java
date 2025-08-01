package ru.krotarnya.diasync.wear.model;

import android.graphics.Canvas;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.Data;
import ru.krotarnya.diasync.common.model.BatteryStatus;
import ru.krotarnya.diasync.common.repository.DataPoint;
import ru.krotarnya.diasync.common.repository.Settings;

@Data
@Builder
public class WatchFaceData {
    @Nullable
    private final List<DataPoint> dataPoints;
    @Nullable
    private final BatteryStatus batteryStatus;
    @Nullable
    private final Settings settings;
    @Nullable
    private final Long stepsToday;

    // never set through builder()
    private Canvas canvas;
    private Rect bounds;
    private ZonedDateTime now;

    public Settings getSettings() {
        return Optional.ofNullable(settings).orElseGet(Settings::getDefault);
    }
}
