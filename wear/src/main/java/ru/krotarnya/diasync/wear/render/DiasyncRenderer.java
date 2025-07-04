package ru.krotarnya.diasync.wear.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.time.ZonedDateTime;
import java.util.List;

import ru.krotarnya.diasync.common.events.BatteryStatusEvent;
import ru.krotarnya.diasync.common.events.NewDataEvent;
import ru.krotarnya.diasync.common.model.WidgetData;
import ru.krotarnya.diasync.common.service.WidgetDataProvider;

/**
 * @noinspection deprecation
 */
public final class DiasyncRenderer extends Renderer.CanvasRenderer {
    private static final int BACKGROUND_COLOR = Color.BLACK;

    private final WidgetDataProvider dataProvider;
    private final List<ComponentRenderer> componentRenderers;
    private WidgetData widgetData;

    public DiasyncRenderer(
            @NonNull WidgetDataProvider dataProvider,
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

        this.dataProvider = dataProvider;

        this.componentRenderers = List.of(
                new DateTimeRenderer(),
                new BatteryRenderer(),
                new StaleRenderer());

        updateWidgetData();
        EventBus.getDefault().register(this);
    }


    @Override
    public void render(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
        canvas.drawColor(BACKGROUND_COLOR);
        componentRenderers.forEach(renderer -> renderer.render(canvas, bounds, zonedDateTime, widgetData));
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNewData(NewDataEvent event) {
        updateWidgetData();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onBatteryChange(BatteryStatusEvent event) {
        updateWidgetData();
    }

    private void updateWidgetData() {
        widgetData = dataProvider.get();
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
        EventBus.getDefault().unregister(this);
    }
}
