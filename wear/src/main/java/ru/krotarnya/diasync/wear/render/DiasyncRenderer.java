package ru.krotarnya.diasync.wear.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import java.time.ZonedDateTime;
import java.util.List;

import ru.krotarnya.diasync.wear.model.WatchFace;
import ru.krotarnya.diasync.wear.service.WatchFaceHolder;

/**
 * @noinspection deprecation
 */
public final class DiasyncRenderer extends Renderer.CanvasRenderer {
    private static final int BACKGROUND_COLOR = Color.BLACK;

    private final WatchFaceHolder watchFaceHolder;
    private final List<ComponentRenderer> componentRenderers;

    public DiasyncRenderer(
            @NonNull WatchFaceHolder watchFaceHolder,
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository,
            @NonNull WatchState watchState,
            int canvasType,
            long interactiveDrawModeUpdateDelayMillis)
    {
        super(
                surfaceHolder,
                currentUserStyleRepository,
                watchState,
                canvasType,
                interactiveDrawModeUpdateDelayMillis);

        this.watchFaceHolder = watchFaceHolder;
        this.componentRenderers = List.of(
                new DateTimeRenderer(),
                new BatteryRenderer(),
                new StaleRenderer(),
                new ChartRenderer());
    }


    @Override
    public void render(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
        canvas.drawColor(BACKGROUND_COLOR);

        WatchFace data = watchFaceHolder.get();
        data.setCanvas(canvas);
        data.setBounds(bounds);
        data.setNow(zonedDateTime);

        componentRenderers.forEach(renderer -> renderer.render(data));
    }

    @Override
    public void renderHighlightLayer(
            @NonNull Canvas canvas,
            @NonNull Rect bounds,
            @NonNull ZonedDateTime zonedDateTime)
    {

    }
}
