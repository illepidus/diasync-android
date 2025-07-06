package ru.krotarnya.diasync.wear.render;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.CanvasType;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import java.time.ZonedDateTime;

import kotlin.coroutines.Continuation;

public abstract class SimpleCanvasRender extends Renderer.CanvasRenderer2<Renderer.SharedAssets> {
    public SimpleCanvasRender(
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository,
            @NonNull WatchState watchState)
    {
        super(
                surfaceHolder,
                currentUserStyleRepository,
                watchState,
                CanvasType.HARDWARE,
                100L,
                false);
    }

    public abstract void doRender(Canvas canvas, Rect bounds, ZonedDateTime zonedDateTime);

    @Override
    public final void render(
            @NonNull Canvas canvas,
            @NonNull Rect rect,
            @NonNull ZonedDateTime zonedDateTime,
            @NonNull SharedAssets sharedAssets)
    {
        doRender(canvas, rect, zonedDateTime);
    }

    @Override
    protected final Renderer.SharedAssets createSharedAssets(@NonNull Continuation<? super SharedAssets> continuation)
    {
        return () -> {};
    }

    @Override
    public final void renderHighlightLayer(
            @NonNull Canvas canvas,
            @NonNull Rect rect,
            @NonNull ZonedDateTime zonedDateTime,
            @NonNull Renderer.SharedAssets sharedAssets)
    {

    }
}
