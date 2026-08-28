package ru.krotarnya.diasync2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAppWidgetManager;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.settings.GraphWindow;
import ru.krotarnya.diasync2.settings.WidgetSettings;
import ru.krotarnya.diasync2.settings.WatchSettings;
import ru.krotarnya.diasync2.sync.MonitoringService;
import ru.krotarnya.diasync2.sync.SyncConnectionState;
import ru.krotarnya.diasync2.widget.DiasyncWidgetProvider;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class MainActivityWidgetSettingsTest {
    @Test
    public void monitoringUsesOneContextAwareToggleButton() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        Button toggle = activity.findViewById(R.id.monitoring_toggle);

        assertEquals(application.getString(R.string.start_monitoring), toggle.getText().toString());

        activity.onSyncStateChanged(SyncConnectionState.CONNECTED, false);

        assertEquals(application.getString(R.string.stop_monitoring), toggle.getText().toString());
        toggle.performClick();
        Intent serviceIntent = shadowOf(application).getNextStartedService();
        assertEquals(MonitoringService.ACTION_STOP, serviceIntent.getAction());
        assertEquals(application.getString(R.string.start_monitoring), toggle.getText().toString());
    }

    @Test
    public void changingUnitConvertsThresholdFieldsAndRequestsWidgetUpdate() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        new AppPreferences(application).saveWidgetSettings(WidgetSettings.defaults());
        AppWidgetManager manager = AppWidgetManager.getInstance(application);
        ShadowAppWidgetManager shadowManager = shadowOf(manager);
        shadowManager.createWidget(
                DiasyncWidgetProvider.class,
                R.layout.widget_latest_value);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        Spinner unit = activity.findViewById(R.id.glucose_unit);
        EditText low = activity.findViewById(R.id.low_threshold);
        EditText high = activity.findViewById(R.id.high_threshold);
        int broadcastCount = shadowOf(application).getBroadcastIntents().size();

        unit.setSelection(1);

        assertEquals("70", low.getText().toString());
        assertEquals("180", high.getText().toString());
        List<Intent> broadcasts = shadowOf(application).getBroadcastIntents();
        assertTrue(broadcasts.size() > broadcastCount);
        assertEquals(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                broadcasts.get(broadcasts.size() - 1).getAction());

        unit.setSelection(0);

        assertEquals("3.9", low.getText().toString());
        assertEquals("10.0", high.getText().toString());

        int afterUnitChange = shadowOf(application).getBroadcastIntents().size();
        low.setText("4.0");
        assertTrue(shadowOf(application).getBroadcastIntents().size() > afterUnitChange);

        int afterThresholdChange = shadowOf(application).getBroadcastIntents().size();
        Spinner window = activity.findViewById(R.id.widget_graph_window);
        window.setSelection(1);
        assertTrue(shadowOf(application).getBroadcastIntents().size() > afterThresholdChange);

        int afterWindowChange = shadowOf(application).getBroadcastIntents().size();
        CheckBox zones = activity.findViewById(R.id.widget_graph_zones);
        zones.performClick();
        assertTrue(shadowOf(application).getBroadcastIntents().size() > afterWindowChange);

        int afterZonesChange = shadowOf(application).getBroadcastIntents().size();
        CheckBox trendArrow = activity.findViewById(R.id.widget_trend_arrow);
        trendArrow.performClick();
        assertTrue(shadowOf(application).getBroadcastIntents().size() > afterZonesChange);

        WidgetSettings saved = new AppPreferences(application).loadWidgetSettings();
        assertEquals(72.0, saved.lowMgDl(), 0.01);
        assertEquals(GraphWindow.ONE_HOUR, saved.graphWindow());
        assertFalse(saved.graphZones());
        assertFalse(saved.trendArrow());
    }

    @Test
    public void watchControlsPersistWithoutChangingWidgetSettings() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        AppPreferences preferences = new AppPreferences(application);
        WidgetSettings widgetSettings = WidgetSettings.defaults();
        preferences.saveWidgetSettings(widgetSettings);
        preferences.saveWatchSettings(WatchSettings.defaults());
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        Spinner watchWindow = activity.findViewById(R.id.watch_graph_window);
        CheckBox watchZones = activity.findViewById(R.id.watch_graph_zones);
        CheckBox watchLines = activity.findViewById(R.id.watch_graph_lines);
        CheckBox watchTrend = activity.findViewById(R.id.watch_trend_arrow);
        watchWindow.setSelection(2);
        watchZones.performClick();
        watchLines.performClick();
        watchTrend.performClick();

        assertEquals(
                new WatchSettings(GraphWindow.THREE_HOURS, false, true, false),
                preferences.loadWatchSettings());
        assertEquals(widgetSettings, preferences.loadWidgetSettings());
    }
}
