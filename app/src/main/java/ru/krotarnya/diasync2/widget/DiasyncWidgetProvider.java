package ru.krotarnya.diasync2.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.krotarnya.diasync2.DiasyncApplication;
import ru.krotarnya.diasync2.MainActivity;
import ru.krotarnya.diasync2.navigation.PhoneScreen;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.WidgetSettings;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.settings.WidgetClickAction;

public final class DiasyncWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_WIDGET_TAP = "ru.krotarnya.diasync2.action.WIDGET_TAP";
    static final int MAX_PRESENTATION_POINTS = 256;
    private static final Handler TAP_HANDLER = new Handler(Looper.getMainLooper());
    private static final WidgetTapRouter TAP_ROUTER = new WidgetTapRouter();

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
        if (ACTION_WIDGET_TAP.equals(intent.getAction())) {
            routeTap(context);
            return;
        }
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

    private void routeTap(Context context) {
        long tapAt = SystemClock.elapsedRealtime();
        if (TAP_ROUTER.tap(tapAt) == WidgetTapRouter.Result.OPEN_ALERTS) {
            open(context, new AppPreferences(context).loadWidgetDoubleClickAction());
            return;
        }
        TAP_HANDLER.postDelayed(() -> {
            if (TAP_ROUTER.consumeSingleTap(tapAt)) {
                open(context, new AppPreferences(context).loadWidgetSingleClickAction());
            }
        }, WidgetTapRouter.DOUBLE_TAP_WINDOW_MILLIS);
    }

    private static void open(Context context, WidgetClickAction action) {
        if (action == WidgetClickAction.NONE) {
            return;
        }
        if (action == WidgetClickAction.XDRIP) {
            Intent xdrip = context.getPackageManager().getLaunchIntentForPackage(
                    "com.eveningoutpost.dexdrip");
            if (xdrip != null) {
                context.startActivity(xdrip.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            }
        }
        PhoneScreen screen = action == WidgetClickAction.ALERTS
                ? PhoneScreen.ALERTS
                : PhoneScreen.STATUS;
        context.startActivity(new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_SCREEN, screen.name())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP));
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
                    updateWidget(context, manager, appWidgetId, state);
                }
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void updateWidget(
            Context context,
            AppWidgetManager manager,
            int appWidgetId,
            WidgetState state
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<WidgetSizeOption> options = WidgetSizeOption.from(
                    manager.getAppWidgetOptions(appWidgetId),
                    context.getResources().getDisplayMetrics().density);
            if (!options.isEmpty()) {
                Map<SizeF, RemoteViews> exactViews = new ArrayMap<>();
                for (WidgetSizeOption option : options) {
                    WidgetBitmapSize size = option.bitmapSize();
                    exactViews.put(
                            option.hostSize(),
                            viewsFactory.create(context, state, renderGraph(size, state), size));
                }
                manager.updateAppWidget(appWidgetId, new RemoteViews(exactViews));
                return;
            }
        }
        WidgetBitmapSize size = widgetSize(context, manager, appWidgetId);
        manager.updateAppWidget(
                appWidgetId,
                viewsFactory.create(context, state, renderGraph(size, state), size));
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
