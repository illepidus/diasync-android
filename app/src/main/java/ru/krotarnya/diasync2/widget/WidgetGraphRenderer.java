package ru.krotarnya.diasync2.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.Objects;

final class WidgetGraphRenderer {
    static final int LOW_COLOR = Color.rgb(195, 9, 9);
    static final int NORMAL_COLOR = Color.rgb(0, 191, 255);
    static final int HIGH_COLOR = Color.rgb(255, 187, 51);
    static final int LOW_ZONE_COLOR = Color.argb(64, 195, 9, 9);
    static final int NORMAL_ZONE_COLOR = Color.argb(64, 0, 0, 0);
    static final int HIGH_ZONE_COLOR = Color.argb(64, 255, 187, 51);

    Bitmap render(WidgetGraphLayout layout, boolean zones, boolean lines) {
        Objects.requireNonNull(layout);
        Bitmap bitmap = Bitmap.createBitmap(
                layout.width(),
                layout.height(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        if (zones) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(HIGH_ZONE_COLOR);
            canvas.drawRect(0, 0, layout.width(), (float) layout.highY(), paint);
            paint.setColor(NORMAL_ZONE_COLOR);
            canvas.drawRect(
                    0,
                    (float) layout.highY(),
                    layout.width(),
                    (float) layout.lowY(),
                    paint);
            paint.setColor(LOW_ZONE_COLOR);
            canvas.drawRect(0, (float) layout.lowY(), layout.width(), layout.height(), paint);
        }

        if (lines) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.0f, Math.min(layout.width(), layout.height()) / 100.0f));
            paint.setColor(HIGH_COLOR);
            canvas.drawLine(0, (float) layout.highY(), layout.width(), (float) layout.highY(), paint);
            paint.setColor(LOW_COLOR);
            canvas.drawLine(0, (float) layout.lowY(), layout.width(), (float) layout.lowY(), paint);
        }

        paint.setStyle(Paint.Style.FILL);
        for (WidgetGraphLayout.Point point : layout.points()) {
            paint.setColor(color(point.range()));
            canvas.drawCircle(point.x(), point.y(), layout.pointRadius(), paint);
        }
        return bitmap;
    }

    private int color(WidgetState.Range range) {
        return switch (range) {
            case LOW -> LOW_COLOR;
            case NORMAL -> NORMAL_COLOR;
            case HIGH -> HIGH_COLOR;
            case ERROR -> Color.WHITE;
        };
    }
}
