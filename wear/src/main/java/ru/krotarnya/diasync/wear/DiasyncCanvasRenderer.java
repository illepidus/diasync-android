package ru.krotarnya.diasync.wear;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import java.time.ZonedDateTime;

import ru.krotarnya.diasync.common.service.WidgetDataProvider;

/**
 * @noinspection deprecation
 */
class DiasyncCanvasRenderer extends Renderer.CanvasRenderer {
    private final WidgetDataProvider dataProvider;
    private final Paint hourMinutePaint;
    private final Paint bloodPaint;

    public DiasyncCanvasRenderer(
            @NonNull WidgetDataProvider dataProvider,
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository,
            @NonNull WatchState watchState,
            int canvasType,
            long interactiveDrawModeUpdateDelayMillis) {
        super(
                surfaceHolder,
                currentUserStyleRepository,
                watchState,
                canvasType,
                interactiveDrawModeUpdateDelayMillis);

        this.dataProvider = dataProvider;

        hourMinutePaint = new Paint();
        hourMinutePaint.setColor(Color.CYAN);
        hourMinutePaint.setTextSize(60f);
        hourMinutePaint.setAntiAlias(true);

        bloodPaint = new Paint();
        bloodPaint.setColor(Color.MAGENTA);
        bloodPaint.setTextSize(120f);
        bloodPaint.setAntiAlias(true);
    }

    @Override
    @SuppressLint("DefaultLocale")
    public void render(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
        canvas.drawColor(Color.BLACK);
        String timeText = String.format("%02d:%02d:%02d", zonedDateTime.getHour(), zonedDateTime.getMinute(), zonedDateTime.getSecond());
        float centerX = bounds.width() / 2f;
        float centerY = bounds.height() / 2f;
        float timeWidth = hourMinutePaint.measureText(timeText);
        canvas.drawText(timeText, centerX - timeWidth / 2, centerY / 1.8f, hourMinutePaint);

        dataProvider.get()
                .getDataPoints()
                .stream()
                .findAny()
                .map(p -> p.sensorGlucose)
                .map(g -> g.mgdl)
                .ifPresent(mgdl -> {
                    String bloodText = String.format("%.1f", mgdl / 18.018);
                    float bloodWidth = bloodPaint.measureText(bloodText);

                    canvas.drawText(
                            bloodText,
                            centerX - bloodWidth / 2,
                            centerY + 40,
                            bloodPaint);
                });
    }

    @Override
    public void renderHighlightLayer(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
    }
}
