package ru.krotarnya.diasync.wear.render;

import android.graphics.Color;
import android.graphics.Paint;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import ru.krotarnya.diasync.common.repository.DataPoint;
import ru.krotarnya.diasync.wear.model.WatchFace;

final class StaleRenderer implements ComponentRenderer {
    private static final String NO_DATA = "No data";
    private static final Duration STALE_WARNING_THRESHOLD = Duration.ofSeconds(90);

    private final Paint paint;

    public StaleRenderer() {
        paint = new Paint();
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.RED);
    }

    @Override
    public void render(WatchFace watchFace) {
        paint.setTextSize(watchFace.getBounds().height() / 10f);

        Optional<Instant> lastPoint = Optional.ofNullable(watchFace.getDataPoints())
                .stream()
                .flatMap(Collection::stream)
                .filter(p -> p.getSensorGlucose() != null)
                .max(Comparator.comparing(DataPoint::getTimestamp))
                .map(DataPoint::getTimestamp);

        String text = lastPoint
                .map(p -> Duration.between(p, watchFace.getZonedDateTime()))
                .map(d -> d.compareTo(STALE_WARNING_THRESHOLD) > 0 ? d.getSeconds() / 60 + "m" : "")
                .orElse(NO_DATA);

        watchFace.getCanvas().drawText(
                text,
                watchFace.getBounds().centerX(),
                watchFace.getBounds().height() * 0.9f - (paint.descent() + paint.ascent()) / 2,
                paint);
    }
}
