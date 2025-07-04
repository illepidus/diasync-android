package ru.krotarnya.diasync.wear.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.time.ZonedDateTime;
import java.util.Optional;

import ru.krotarnya.diasync.common.model.BatteryStatus;
import ru.krotarnya.diasync.common.model.WidgetData;

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
    public void render(Canvas canvas, Rect bounds, ZonedDateTime zonedDateTime, WidgetData widgetData) {
        Optional.ofNullable(widgetData.getBatteryStatus())
                .ifPresent(batteryStatus -> {
                    paint.setColor(getColor(batteryStatus));
                    paint.setTextAlign(Paint.Align.LEFT);
                    paint.setTextSize(bounds.height() / 15f);

                    canvas.drawText(
                            batteryStatus.chargePercentRounded() + "%",
                            (int) (bounds.width() * 0.1),
                            bounds.height() * 0.28f - (paint.descent() + paint.ascent()) / 2,
                            paint);
                });
    }

    private int getColor(BatteryStatus batteryStatus) {
        if (batteryStatus.isCharging()) return BATTERY_CHARGING_COLOR;
        if (batteryStatus.chargePercentRounded() <= BATTERY_CRITICAL_PERCENT) return BATTERY_CRITICAL_COLOR;
        return BATTERY_NORMAL_COLOR;
    }
}
