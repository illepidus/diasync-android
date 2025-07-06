package ru.krotarnya.diasync.wear.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import java.time.ZonedDateTime;
import java.util.List;

import ru.krotarnya.diasync.wear.model.WatchFaceData;
import ru.krotarnya.diasync.wear.service.WatchFaceDataAccessor;

public final class DiasyncRenderer extends SimpleCanvasRender {
    private static final int BACKGROUND_COLOR = Color.BLACK;

    private final WatchFaceDataAccessor watchFaceDataAccessor;
    private final List<ComponentRenderer> componentRenderers;

    public DiasyncRenderer(
            WatchFaceDataAccessor dataAccessor,
            SurfaceHolder surfaceHolder,
            CurrentUserStyleRepository styleRepository,
            WatchState watchState)
    {
        super(surfaceHolder, styleRepository, watchState);

        this.watchFaceDataAccessor = dataAccessor;
        this.componentRenderers = List.of(
                new DateTimeRenderer(),
                new BatteryRenderer(),
                new StepsTodayRenderer(),
                new StaleRenderer(),
                new ChartRenderer());
    }

    @Override
    public void doRender(Canvas canvas, Rect bounds, ZonedDateTime zonedDateTime) {
        canvas.drawColor(BACKGROUND_COLOR);

        WatchFaceData watchFaceData = watchFaceDataAccessor.build();
        watchFaceData.setCanvas(canvas);
        watchFaceData.setBounds(bounds);
        watchFaceData.setNow(zonedDateTime);

        componentRenderers.forEach(renderer -> renderer.render(watchFaceData));
    }
}
