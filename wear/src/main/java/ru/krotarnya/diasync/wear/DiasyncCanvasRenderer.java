package ru.krotarnya.diasync.wear;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.Renderer.CanvasRenderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import java.time.ZonedDateTime;

/**
 * @noinspection deprecation
 */
@SuppressLint("DefaultLocale")
class DiasyncCanvasRenderer extends CanvasRenderer {
    private final Paint hourMinutePaint;
    private final Paint secondPaint;

    public DiasyncCanvasRenderer(
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository,
            @NonNull WatchState watchState,
            int canvasType,
            long interactiveDrawModeUpdateDelayMillis) {
        super(surfaceHolder, currentUserStyleRepository, watchState, canvasType, interactiveDrawModeUpdateDelayMillis);

        hourMinutePaint = new Paint();
        hourMinutePaint.setColor(Color.CYAN);
        hourMinutePaint.setTextSize(60f);
        hourMinutePaint.setAntiAlias(true);
        secondPaint = new Paint();
        secondPaint.setColor(Color.MAGENTA);
        secondPaint.setTextSize(30f);
        secondPaint.setAntiAlias(true);
    }

    @Override
    public void render(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
        canvas.drawColor(Color.BLACK);
        int hours = zonedDateTime.getHour();
        int minutes = zonedDateTime.getMinute();
        int seconds = zonedDateTime.getSecond();
        String timeText = String.format("%02d:%02d", hours, minutes);
        String secondsText = String.format("%02d", seconds);
        float centerX = bounds.width() / 2f;
        float centerY = bounds.height() / 2f;
        float timeWidth = hourMinutePaint.measureText(timeText);
        float secondsWidth = secondPaint.measureText(secondsText);
        canvas.drawText(timeText, centerX - timeWidth / 2, centerY, hourMinutePaint);
        canvas.drawText(secondsText, centerX - secondsWidth / 2, centerY + 40, secondPaint);
    }

    @Override
    public void renderHighlightLayer(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
    }
}
