package ru.krotarnya.diasync.wear.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import ru.krotarnya.diasync.common.model.WidgetData;

final class DateTimeRenderer implements ComponentRenderer {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EE dd.MM");

    private final Paint paint;

    public DateTimeRenderer() {
        paint = new Paint();
        paint.setFakeBoldText(true);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void render(Canvas canvas, Rect bounds, ZonedDateTime zonedDateTime, WidgetData widgetData) {
        paint.setTextSize(bounds.height() / 5f);

        canvas.drawText(
                zonedDateTime.format(TIME_FORMATTER),
                bounds.centerX(),
                bounds.height() * 0.15f - (paint.descent() + paint.ascent()) / 2,
                paint);

        paint.setTextSize(bounds.height() / 15f);
        canvas.drawText(
                zonedDateTime.format(DATE_FORMATTER),
                bounds.centerX(),
                bounds.height() * 0.28f - (paint.descent() + paint.ascent()) / 2,
                paint);
    }
}
