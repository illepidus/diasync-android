package ru.krotarnya.diasync.wear.render;

import android.graphics.Color;
import android.graphics.Paint;

import java.time.format.DateTimeFormatter;

import ru.krotarnya.diasync.wear.model.WatchFace;

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
    public void render(WatchFace watchFace) {
        paint.setTextSize(watchFace.getBounds().height() / 5f);

        watchFace.getCanvas().drawText(
                watchFace.getZonedDateTime().format(TIME_FORMATTER),
                watchFace.getBounds().centerX(),
                watchFace.getBounds().height() * 0.15f - (paint.descent() + paint.ascent()) / 2,
                paint);

        paint.setTextSize(watchFace.getBounds().height() / 15f);

        watchFace.getCanvas().drawText(
                watchFace.getZonedDateTime().format(DATE_FORMATTER),
                watchFace.getBounds().centerX(),
                watchFace.getBounds().height() * 0.28f - (paint.descent() + paint.ascent()) / 2,
                paint);
    }
}
