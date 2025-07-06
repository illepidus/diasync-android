package ru.krotarnya.diasync.wear.render;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.Optional;

import ru.krotarnya.diasync.common.model.BatteryStatus;
import ru.krotarnya.diasync.wear.model.WatchFaceData;

final class BatteryRenderer implements ComponentRenderer {
    private static final int BATTERY_NORMAL_COLOR = Color.WHITE;
    private static final int BATTERY_CHARGING_COLOR = Color.GREEN;
    private static final int BATTERY_CRITICAL_COLOR = Color.RED;
    private static final int BATTERY_CRITICAL_PERCENT = 15;

    private final Paint paint;

    public BatteryRenderer() {
        this.paint = new Paint();
        paint.setFakeBoldText(true);
    }

    @Override
    public void render(WatchFaceData watchFaceData) {
        Optional.ofNullable(watchFaceData.getBatteryStatus())
                .ifPresent(batteryStatus -> {
                    paint.setColor(getColor(batteryStatus));
                    paint.setTextAlign(Paint.Align.LEFT);
                    paint.setTextSize(watchFaceData.getBounds().height() / 15f);

                    watchFaceData.getCanvas().drawText(
                            batteryStatus.chargePercentRounded() + "%",
                            (int) (watchFaceData.getBounds().width() * 0.1),
                            watchFaceData.getBounds().height() * 0.28f - (paint.descent() + paint.ascent()) / 2,
                            paint);
                });
    }

    private int getColor(BatteryStatus batteryStatus) {
        if (batteryStatus.isCharging()) return BATTERY_CHARGING_COLOR;
        if (batteryStatus.chargePercentRounded() <= BATTERY_CRITICAL_PERCENT) return BATTERY_CRITICAL_COLOR;
        return BATTERY_NORMAL_COLOR;
    }
}
