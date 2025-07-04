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
import ru.krotarnya.diasync.wear.service.WatchDataHolder;

/**
 * @noinspection deprecation
 */
public final class DiasyncRenderer extends Renderer.CanvasRenderer {
    private static final int BACKGROUND_COLOR = Color.BLACK;

    private final WatchDataHolder dataProvider;
    private final List<ComponentRenderer> componentRenderers;

    public DiasyncRenderer(
            @NonNull WatchDataHolder dataHolder,
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

        this.dataProvider = dataHolder;

        this.componentRenderers = List.of(
                new DateTimeRenderer(),
                new BatteryRenderer(),
                new StaleRenderer());

        //EventBus.getDefault().register(this);
    }


    @Override
    public void render(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
        canvas.drawColor(BACKGROUND_COLOR);

        WatchFace data = dataProvider.get();
        data.setCanvas(canvas);
        data.setBounds(bounds);
        data.setZonedDateTime(zonedDateTime);

        componentRenderers.forEach(renderer -> renderer.render(data));
    }

    @Override
    public void renderHighlightLayer(
            @NonNull Canvas canvas,
            @NonNull Rect bounds,
            @NonNull ZonedDateTime zonedDateTime)
    {

    }

    @Override
    public void onDestroy() {
        //EventBus.getDefault().unregister(this);
    }
}
