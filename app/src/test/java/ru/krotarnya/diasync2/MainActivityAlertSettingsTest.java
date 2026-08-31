package ru.krotarnya.diasync2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.android.material.slider.Slider;
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
        assertTrue(((CheckBox) activity.findViewById(R.id.snooze_wear_alerts)).isChecked());
        Slider slider = activity.findViewById(R.id.snooze_duration);
        assertEquals(0.0f, slider.getValueFrom(), 0.0f);
        assertEquals(12.0f, slider.getValueTo(), 0.0f);
        assertEquals(1.0f, slider.getStepSize(), 0.0f);
    }

    @Test
    public void wearSnoozePreferencePersistsIndependentlyFromPhoneSnooze() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        CheckBox snoozeWearAlerts = controller.get().findViewById(R.id.snooze_wear_alerts);

        snoozeWearAlerts.performClick();

        assertFalse(new AppPreferences(application).snoozeWearAlerts());
        controller.pause().stop().destroy();
        MainActivity recreated = Robolectric.buildActivity(MainActivity.class).setup().get();
        assertFalse(recreated.<CheckBox>findViewById(R.id.snooze_wear_alerts).isChecked());
    }

    @Test
    public void oneContextAwareButtonSnoozesAndResumesImmediately() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        CheckBox low = activity.findViewById(R.id.low_alert_enabled);
        Slider duration = activity.findViewById(R.id.snooze_duration);
        TextView durationLabel = activity.findViewById(R.id.snooze_duration_label);
        Button toggle = activity.findViewById(R.id.snooze_toggle);
        TextView status = activity.findViewById(R.id.snooze_status);
        AppPreferences preferences = new AppPreferences(application);

        assertEquals(application.getString(R.string.snooze_alerts), toggle.getText().toString());
        assertEquals(View.VISIBLE, status.getVisibility());
        assertEquals(
                application.getString(R.string.alerts_active),
                status.getText().toString());
        low.performClick();
        duration.setValue(12.0f);
        Instant before = Instant.now();
        toggle.performClick();

        assertEquals(new AlertSettings(true, false, false), preferences.loadAlertSettings());
        assertTrue(preferences.snoozedUntil().isAfter(before.plus(Duration.ofHours(23))));
        assertEquals(application.getString(R.string.resume_alerts), toggle.getText().toString());
        assertEquals(View.VISIBLE, status.getVisibility());
        assertTrue(status.getText().toString().matches(
                "Phone alerts are snoozed for 2[34]:[0-5][0-9]:[0-5][0-9]"));
        assertEquals(View.GONE, duration.getVisibility());
        assertEquals(View.GONE, durationLabel.getVisibility());

        toggle.performClick();

        assertEquals(Instant.EPOCH, preferences.snoozedUntil());
        assertEquals(application.getString(R.string.snooze_alerts), toggle.getText().toString());
        assertEquals(View.VISIBLE, status.getVisibility());
        assertEquals(
                application.getString(R.string.alerts_active),
                status.getText().toString());
        assertEquals(View.VISIBLE, duration.getVisibility());
        assertEquals(View.VISIBLE, durationLabel.getVisibility());
    }

    @Test
    public void selectedSnoozeDurationSurvivesActivityRecreation() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        ActivityController<MainActivity> firstController =
                Robolectric.buildActivity(MainActivity.class).setup();
        Slider firstSlider = firstController.get().findViewById(R.id.snooze_duration);

        firstSlider.setValue(9.0f);
        firstController.pause().stop().destroy();
        MainActivity recreated = Robolectric.buildActivity(MainActivity.class).setup().get();

        assertEquals(9.0f, recreated.<Slider>findViewById(R.id.snooze_duration)
                .getValue(), 0.0f);
        assertEquals(
                SnoozeOption.EIGHT_HOURS,
                new AppPreferences(application).loadSnoozeOption());
    }

    @Test
    public void activeSnoozeHidesDurationControlsAfterActivityRecreation() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        ActivityController<MainActivity> firstController =
                Robolectric.buildActivity(MainActivity.class).setup();

        firstController.get().<Button>findViewById(R.id.snooze_toggle).performClick();
        firstController.pause().stop().destroy();
        MainActivity recreated = Robolectric.buildActivity(MainActivity.class).setup().get();

        assertEquals(View.GONE,
                recreated.findViewById(R.id.snooze_duration).getVisibility());
        assertEquals(View.GONE,
                recreated.findViewById(R.id.snooze_duration_label).getVisibility());
        assertEquals(
                application.getString(R.string.resume_alerts),
                recreated.<Button>findViewById(R.id.snooze_toggle).getText().toString());
    }
}
