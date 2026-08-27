package ru.krotarnya.diasync2.settings;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Optional;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.sync.SyncConnectionState;

public final class AppPreferences {
    private static final String FILE_NAME = "diasync_settings";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_UNIT = "unit";
    private static final String KEY_USE_CALIBRATION = "use_calibration";
    private static final String KEY_LOW_MG_DL = "low_mg_dl";
    private static final String KEY_HIGH_MG_DL = "high_mg_dl";
    private static final String KEY_WIDGET_GRAPH_WINDOW = "widget_graph_window";
    private static final String KEY_WIDGET_GRAPH_ZONES = "widget_graph_zones";
    private static final String KEY_WIDGET_GRAPH_LINES = "widget_graph_lines";
    private static final String KEY_WIDGET_TREND_ARROW = "widget_trend_arrow";
    private static final String KEY_MONITORING_ENABLED = "monitoring_enabled";
    private static final String KEY_SYNC_CONNECTION_STATE = "sync_connection_state";

    private final SharedPreferences preferences;

    public AppPreferences(Context context) {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public Optional<AppConfiguration> load() {
        String baseUrl = preferences.getString(KEY_BASE_URL, "");
        String userId = preferences.getString(KEY_USER_ID, "");
        if (baseUrl.isBlank() || userId.isBlank()) {
            return Optional.empty();
        }
        WidgetSettings widgetSettings = loadWidgetSettings();
        return Optional.of(new AppConfiguration(
                baseUrl,
                userId,
                widgetSettings.unit(),
                widgetSettings.useCalibration(),
                widgetSettings.lowMgDl(),
                widgetSettings.highMgDl(),
                widgetSettings.graphWindow(),
                widgetSettings.graphZones(),
                widgetSettings.graphLines(),
                widgetSettings.trendArrow()));
    }

    public WidgetSettings loadWidgetSettings() {
        GlucoseUnit unit;
        try {
            unit = GlucoseUnit.valueOf(
                    preferences.getString(KEY_UNIT, GlucoseUnit.MMOL_L.name()));
        } catch (IllegalArgumentException exception) {
            unit = GlucoseUnit.MMOL_L;
        }
        GraphWindow graphWindow;
        try {
            graphWindow = GraphWindow.valueOf(preferences.getString(
                    KEY_WIDGET_GRAPH_WINDOW,
                    GraphWindow.THIRTY_MINUTES.name()));
        } catch (IllegalArgumentException exception) {
            graphWindow = GraphWindow.THIRTY_MINUTES;
        }
        double lowMgDl = validThreshold(
                preferences.getFloat(KEY_LOW_MG_DL, (float) AppConfiguration.DEFAULT_LOW_MG_DL),
                AppConfiguration.DEFAULT_LOW_MG_DL);
        double highMgDl = validThreshold(
                preferences.getFloat(KEY_HIGH_MG_DL, (float) AppConfiguration.DEFAULT_HIGH_MG_DL),
                AppConfiguration.DEFAULT_HIGH_MG_DL);
        if (lowMgDl >= highMgDl) {
            lowMgDl = AppConfiguration.DEFAULT_LOW_MG_DL;
            highMgDl = AppConfiguration.DEFAULT_HIGH_MG_DL;
        }
        return new WidgetSettings(
                unit,
                preferences.getBoolean(KEY_USE_CALIBRATION, true),
                lowMgDl,
                highMgDl,
                graphWindow,
                preferences.getBoolean(KEY_WIDGET_GRAPH_ZONES, true),
                preferences.getBoolean(KEY_WIDGET_GRAPH_LINES, false),
                preferences.getBoolean(KEY_WIDGET_TREND_ARROW, true));
    }

    public void save(AppConfiguration configuration) {
        preferences.edit()
                .putString(KEY_BASE_URL, configuration.baseUrl())
                .putString(KEY_USER_ID, configuration.userId())
                .putString(KEY_UNIT, configuration.unit().name())
                .putBoolean(KEY_USE_CALIBRATION, configuration.useCalibration())
                .putFloat(KEY_LOW_MG_DL, (float) configuration.lowMgDl())
                .putFloat(KEY_HIGH_MG_DL, (float) configuration.highMgDl())
                .putString(KEY_WIDGET_GRAPH_WINDOW, configuration.widgetGraphWindow().name())
                .putBoolean(KEY_WIDGET_GRAPH_ZONES, configuration.widgetGraphZones())
                .putBoolean(KEY_WIDGET_GRAPH_LINES, configuration.widgetGraphLines())
                .putBoolean(KEY_WIDGET_TREND_ARROW, configuration.widgetTrendArrow())
                .apply();
    }

    public void saveWidgetSettings(WidgetSettings settings) {
        preferences.edit()
                .putString(KEY_UNIT, settings.unit().name())
                .putBoolean(KEY_USE_CALIBRATION, settings.useCalibration())
                .putFloat(KEY_LOW_MG_DL, (float) settings.lowMgDl())
                .putFloat(KEY_HIGH_MG_DL, (float) settings.highMgDl())
                .putString(KEY_WIDGET_GRAPH_WINDOW, settings.graphWindow().name())
                .putBoolean(KEY_WIDGET_GRAPH_ZONES, settings.graphZones())
                .putBoolean(KEY_WIDGET_GRAPH_LINES, settings.graphLines())
                .putBoolean(KEY_WIDGET_TREND_ARROW, settings.trendArrow())
                .apply();
    }

    private double validThreshold(float value, double fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    public boolean monitoringEnabled() {
        return preferences.getBoolean(KEY_MONITORING_ENABLED, false);
    }

    public void setMonitoringEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply();
    }

    public SyncConnectionState syncConnectionState() {
        try {
            return SyncConnectionState.valueOf(preferences.getString(
                    KEY_SYNC_CONNECTION_STATE,
                    SyncConnectionState.STOPPED.name()));
        } catch (IllegalArgumentException exception) {
            return SyncConnectionState.STOPPED;
        }
    }

    public void saveSyncConnectionState(SyncConnectionState state) {
        preferences.edit().putString(KEY_SYNC_CONNECTION_STATE, state.name()).apply();
    }
}
