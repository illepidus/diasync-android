package ru.krotarnya.diasync2.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.PowerManager;
import java.util.List;
import java.util.Optional;
import ru.krotarnya.diasync2.DiasyncApplication;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.WidgetSettings;

public final class DiasyncWidgetProvider extends AppWidgetProvider {
    static final int MAX_PRESENTATION_POINTS = 256;

    private final WidgetMinuteScheduler minuteScheduler = new WidgetMinuteScheduler();
    private final WidgetRemoteViewsFactory viewsFactory = new WidgetRemoteViewsFactory();
    private final WidgetGraphLayoutCalculator graphLayoutCalculator = new WidgetGraphLayoutCalculator();
    private final WidgetGraphRenderer graphRenderer = new WidgetGraphRenderer();

    public static void requestUpdate(Context context) {
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, DiasyncWidgetProvider.class));
        if (appWidgetIds.length == 0) {
            return;
        }
        Intent intent = new Intent(context, DiasyncWidgetProvider.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WidgetMinuteScheduler.ACTION_MINUTE_TICK.equals(intent.getAction())) {
            if (hasWidgets(context)) {
                PowerManager powerManager = context.getSystemService(PowerManager.class);
                if (powerManager.isInteractive()) {
                    updateAsync(context, allWidgetIds(context), goAsync());
                }
                minuteScheduler.schedule(context);
            }
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAsync(context, appWidgetIds, goAsync());
        minuteScheduler.schedule(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager appWidgetManager,
            int appWidgetId,
            android.os.Bundle newOptions
    ) {
        updateAsync(context, new int[]{appWidgetId}, goAsync());
    }

    @Override
    public void onEnabled(Context context) {
        minuteScheduler.schedule(context);
    }

    @Override
    public void onDisabled(Context context) {
        minuteScheduler.cancel(context);
    }

    private void updateAsync(Context context, int[] appWidgetIds, PendingResult pendingResult) {
        DiasyncApplication application = (DiasyncApplication) context.getApplicationContext();
        application.widgetExecutor().execute(() -> {
            try {
                WidgetState state = loadState(application);
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                for (int appWidgetId : appWidgetIds) {
                    WidgetBitmapSize size = widgetSize(context, manager, appWidgetId);
                    Bitmap graphBitmap = renderGraph(size, state);
                    manager.updateAppWidget(
                            appWidgetId,
                            viewsFactory.create(context, state, graphBitmap, size));
                }
            } finally {
                pendingResult.finish();
            }
        });
    }

    private WidgetState loadState(DiasyncApplication application) {
        Optional<AppConfiguration> saved = application.preferences().load();
        if (saved.isEmpty()) {
            WidgetSettings settings = application.preferences().loadWidgetSettings();
            return application.widgetPresenter().present(
                    List.of(),
                    new AppConfiguration(
                            "",
                            "",
                            settings.unit(),
                            settings.useCalibration(),
                            settings.lowMgDl(),
                            settings.highMgDl(),
                            settings.graphWindow(),
                            settings.graphZones(),
                            settings.graphLines(),
                            settings.trendArrow()));
        }
        AppConfiguration configuration = saved.get();
        List<DataPoint> points = application.bootstrapRepository().latestLocalSensorPoints(
                configuration.userId(),
                MAX_PRESENTATION_POINTS);
        return application.widgetPresenter().present(
                points,
                configuration);
    }

    private WidgetBitmapSize widgetSize(
            Context context,
            AppWidgetManager manager,
            int appWidgetId
    ) {
        return WidgetBitmapSize.from(
                manager.getAppWidgetOptions(appWidgetId),
                context.getResources().getDisplayMetrics().density,
                context.getResources().getConfiguration().orientation
                        != Configuration.ORIENTATION_LANDSCAPE);
    }

    private Bitmap renderGraph(WidgetBitmapSize size, WidgetState state) {
        if (!state.graphVisible()) {
            return null;
        }
        WidgetGraphLayout layout = graphLayoutCalculator.calculate(
                state.graphSamples(),
                state.generatedAt(),
                state.graphWindow().duration(),
                state.lowMgDl(),
                state.highMgDl(),
                size.width(),
                size.height());
        return graphRenderer.render(layout, state.graphZones(), state.graphLines());
    }

    private boolean hasWidgets(Context context) {
        return allWidgetIds(context).length > 0;
    }

    private int[] allWidgetIds(Context context) {
        return AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, DiasyncWidgetProvider.class));
    }
}
