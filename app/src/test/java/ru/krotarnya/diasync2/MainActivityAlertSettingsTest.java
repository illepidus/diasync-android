package ru.krotarnya.diasync2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View;
import java.time.Duration;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import ru.krotarnya.diasync2.settings.AlertSettings;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.settings.SnoozeOption;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class MainActivityAlertSettingsTest {
    @Test
    public void alertsDefaultOffAndEverySnoozeOptionIsAvailable() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();

        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        assertFalse(((CheckBox) activity.findViewById(R.id.low_alert_enabled)).isChecked());
        assertFalse(((CheckBox) activity.findViewById(R.id.high_alert_enabled)).isChecked());
        assertFalse(((CheckBox) activity.findViewById(R.id.no_data_alert_enabled)).isChecked());
        assertEquals(13, ((Spinner) activity.findViewById(R.id.snooze_duration)).getCount());
    }

    @Test
    public void oneContextAwareButtonSnoozesAndResumesImmediately() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        CheckBox low = activity.findViewById(R.id.low_alert_enabled);
        Spinner duration = activity.findViewById(R.id.snooze_duration);
        Button toggle = activity.findViewById(R.id.snooze_toggle);
        TextView status = activity.findViewById(R.id.snooze_status);
        AppPreferences preferences = new AppPreferences(application);

        assertEquals(application.getString(R.string.snooze_alerts), toggle.getText().toString());
        assertEquals(View.GONE, status.getVisibility());
        low.performClick();
        duration.setSelection(12);
        Instant before = Instant.now();
        toggle.performClick();

        assertEquals(new AlertSettings(true, false, false), preferences.loadAlertSettings());
        assertTrue(preferences.snoozedUntil().isAfter(before.plus(Duration.ofHours(23))));
        assertEquals(application.getString(R.string.resume_alerts), toggle.getText().toString());
        assertEquals(View.VISIBLE, status.getVisibility());
        assertTrue(status.getText().toString().matches(
                "All alerts snoozed for 2[34]:[0-5][0-9]:[0-5][0-9]"));

        toggle.performClick();

        assertEquals(Instant.EPOCH, preferences.snoozedUntil());
        assertEquals(application.getString(R.string.snooze_alerts), toggle.getText().toString());
        assertEquals(View.GONE, status.getVisibility());
    }

    @Test
    public void selectedSnoozeDurationSurvivesActivityRecreation() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        ActivityController<MainActivity> firstController =
                Robolectric.buildActivity(MainActivity.class).setup();
        Spinner firstSpinner = firstController.get().findViewById(R.id.snooze_duration);

        firstSpinner.setSelection(9);
        firstController.pause().stop().destroy();
        MainActivity recreated = Robolectric.buildActivity(MainActivity.class).setup().get();

        assertEquals(9, recreated.<Spinner>findViewById(R.id.snooze_duration)
                .getSelectedItemPosition());
        assertEquals(
                SnoozeOption.EIGHT_HOURS,
                new AppPreferences(application).loadSnoozeOption());
    }
}
