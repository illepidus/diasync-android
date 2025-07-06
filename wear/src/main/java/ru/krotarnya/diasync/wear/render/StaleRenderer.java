package ru.krotarnya.diasync.wear.render;

import android.graphics.Color;
import android.graphics.Paint;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import ru.krotarnya.diasync.common.repository.DataPoint;
import ru.krotarnya.diasync.wear.model.WatchFaceData;

final class StaleRenderer implements ComponentRenderer {
    private static final String STALE_MESSAGE = "STALE";
    private static final Duration STALE_WARNING_THRESHOLD = Duration.ofSeconds(90);

    private final Paint paint;

    public StaleRenderer() {
        paint = new Paint();
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.RED);
    }

    @Override
    public void render(WatchFaceData watchFaceData) {
        paint.setTextSize(watchFaceData.getBounds().height() / 10f);

        Optional<Instant> lastPoint = Optional.ofNullable(watchFaceData.getDataPoints())
                .stream()
                .flatMap(Collection::stream)
                .filter(p -> p.getSensorGlucose() != null)
                .max(Comparator.comparing(DataPoint::getTimestamp))
                .map(DataPoint::getTimestamp);

        String text = lastPoint
                .map(ts -> Duration.between(ts, watchFaceData.getNow()))
                .map(stale -> stale.compareTo(STALE_WARNING_THRESHOLD) > 0 ? stale.getSeconds() / 60 + "m" : "")
                .orElse(STALE_MESSAGE);

        watchFaceData.getCanvas().drawText(
                text,
                watchFaceData.getBounds().centerX(),
                watchFaceData.getBounds().height() * 0.9f - (paint.descent() + paint.ascent()) / 2,
                paint);
    }
}
