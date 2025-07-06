package ru.krotarnya.diasync.wear.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.CanvasType;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import java.time.ZonedDateTime;
import java.util.List;

import kotlin.coroutines.Continuation;
import ru.krotarnya.diasync.wear.model.WatchFace;
import ru.krotarnya.diasync.wear.service.WatchFaceHolder;

public final class DiasyncRenderer extends Renderer.CanvasRenderer2<Renderer.SharedAssets> {
    public static final String TAG = DiasyncRenderer.class.getSimpleName();
    private static final int BACKGROUND_COLOR = Color.BLACK;

    private final WatchFaceHolder watchFaceHolder;
    private final List<ComponentRenderer> componentRenderers;

    public DiasyncRenderer(
            @NonNull WatchFaceHolder watchFaceHolder,
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull CurrentUserStyleRepository userStyleRepository,
            @NonNull WatchState watchState)
    {
        super(
                surfaceHolder,
                userStyleRepository,
                watchState,
                CanvasType.HARDWARE,
                100L,
                false);

        this.watchFaceHolder = watchFaceHolder;
        this.componentRenderers = List.of(
                new DateTimeRenderer(),
                new BatteryRenderer(),
                new StepsTodayRenderer(),
                new StaleRenderer(),
                new ChartRenderer()
        );
    }


    @Override
    protected Renderer.SharedAssets createSharedAssets(@NonNull Continuation<? super Renderer.SharedAssets> continuation)
    {
        return () -> {};
    }

    @Override
    public void render(
            @NonNull Canvas canvas,
            @NonNull Rect bounds,
            @NonNull ZonedDateTime zonedDateTime,
            @NonNull Renderer.SharedAssets sharedAssets)
    {
        canvas.drawColor(BACKGROUND_COLOR);

        WatchFace watchFace = watchFaceHolder.build();
        watchFace.setCanvas(canvas);
        watchFace.setBounds(bounds);
        watchFace.setNow(zonedDateTime);

        componentRenderers.forEach(renderer -> renderer.render(watchFace));
    }

    @Override
    public void renderHighlightLayer(
            @NonNull Canvas canvas,
            @NonNull Rect rect,
            @NonNull ZonedDateTime zonedDateTime,
            @NonNull Renderer.SharedAssets sharedAssets)
    {

    }
}
