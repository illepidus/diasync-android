package ru.krotarnya.diasync.wear.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Optional;

import ru.krotarnya.diasync.common.model.WidgetData;
import ru.krotarnya.diasync.common.repository.DataPoint;

final class StaleRenderer implements ComponentRenderer {
    private static final Duration STALE_WARNING_THRESHOLD = Duration.ofSeconds(90);

    @Override
    public void render(Canvas canvas, Rect bounds, ZonedDateTime zonedDateTime, WidgetData widgetData) {
        Paint paint = new Paint();
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bounds.height() / 10f);

        paint.setColor(Color.RED);
        Optional<Instant> lastPoint = Optional.ofNullable(widgetData)
                .stream()
                .flatMap(c -> c.getDataPoints().stream())
                .filter(p -> p.getSensorGlucose() != null)
                .max(Comparator.comparing(DataPoint::getTimestamp))
                .map(DataPoint::getTimestamp);

        String text = lastPoint
                .map(p -> Duration.between(p, zonedDateTime))
                .map(d -> d.compareTo(STALE_WARNING_THRESHOLD) > 0
                        ? d.getSeconds() / 60 + "m"
                        : "")
                .orElse("NO DATA");

        canvas.drawText(
                text,
                bounds.centerX(),
                bounds.height() * 0.9f - (paint.descent() + paint.ascent()) / 2,
                paint);
    }
}
