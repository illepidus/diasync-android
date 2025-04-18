package ru.krotarnya.diasync.wear;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

import ru.krotarnya.diasync.common.model.DataPoint;

@SuppressLint("DefaultLocale")
class DiasyncCanvasRenderer extends Renderer.CanvasRenderer {
    private final Paint hourMinutePaint;
    private final Paint secondPaint;
    private final Paint bloodPaint;

    @Nullable
    private DataPoint dataPoint;

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

        bloodPaint = new Paint();
        bloodPaint.setColor(Color.MAGENTA);
        bloodPaint.setTextSize(60f);
        bloodPaint.setAntiAlias(true);
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

        Optional.ofNullable(dataPoint)
                .map(p -> p.sensorGlucose)
                .map(g -> g.mgdl)
                .ifPresent(mgdl -> {
                    String bloodText = String.format("%2f", mgdl);
                    float bloodWidth = bloodPaint.measureText(bloodText);

                    canvas.drawText(
                            bloodText,
                            centerX - bloodWidth / 2,
                            centerY + 80,
                            bloodPaint);
                });
    }

    @Override
    public void renderHighlightLayer(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
    }

    public void update(DataPoint dataPoint) {
        this.dataPoint = dataPoint;
    }
}
