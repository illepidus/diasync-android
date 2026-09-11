package ru.krotarnya.diasync2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Intent;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.MotionEvent;
import android.widget.EditText;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import ru.krotarnya.diasync2.navigation.PhoneScreen;
import ru.krotarnya.diasync2.presentation.StatusState;
import ru.krotarnya.diasync2.settings.AppMode;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.sync.SyncConnectionState;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class MainActivityBackNavigationTest {
    @Test
    public void backFromSubmenuReturnsToStatus() {
        Intent intent = new Intent().putExtra(MainActivity.EXTRA_SCREEN, PhoneScreen.ALERTS.name());
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        activity.getOnBackPressedDispatcher().onBackPressed();

        assertEquals(View.VISIBLE, activity.findViewById(R.id.settings_menu).getVisibility());
        assertFalse(activity.isFinishing());
    }

    @Test
    public void backFromStatusDoesNotFinishActivity() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        activity.getOnBackPressedDispatcher().onBackPressed();

        assertFalse(activity.isFinishing());
    }

    @Test
    public void diagnosticsContentIsShownAfterSubmenuUpdate() {
        Intent intent = new Intent().putExtra(MainActivity.EXTRA_SCREEN, PhoneScreen.ALERTS.name());
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        activity.render(StatusState.latest("5.6", "mmol/L", "2026-08-29T12:00:00Z", "Just now"));
        assertEquals(View.GONE, activity.findViewById(R.id.latest_value).getVisibility());

        activity.getOnBackPressedDispatcher().onBackPressed();

        assertEquals(View.GONE, activity.findViewById(R.id.latest_value).getVisibility());

        activity.findViewById(R.id.open_diagnostics).performClick();
        assertEquals(View.GONE, activity.findViewById(R.id.latest_value).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.diagnostics_content).getVisibility());
    }

    @Test
    public void connectionUsesDefaultUrlAndPasswordToggle() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        Intent intent = new Intent().putExtra(
                MainActivity.EXTRA_SCREEN, PhoneScreen.CONNECTION.name());
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        assertEquals("https://diasync.krotarnya.ru",
                ((EditText) activity.findViewById(R.id.backend_url)).getText().toString());
        assertEquals(TextInputLayout.END_ICON_PASSWORD_TOGGLE,
                ((TextInputLayout) activity.findViewById(R.id.user_id_container)).getEndIconMode());
        assertEquals(PasswordTransformationMethod.class,
                ((EditText) activity.findViewById(R.id.user_id)).getTransformationMethod().getClass());
    }

    @Test
    public void activeMonitoringKeepsSelectedModeOnSwitchAttempt() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        Intent intent = new Intent().putExtra(
                MainActivity.EXTRA_SCREEN, PhoneScreen.CONNECTION.name());
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        MaterialButtonToggleGroup mode = activity.findViewById(R.id.app_mode);
        MaterialButton slave = activity.findViewById(R.id.mode_slave);
        MaterialButton master = activity.findViewById(R.id.mode_master);
        EditText backendUrl = activity.findViewById(R.id.backend_url);
        EditText userId = activity.findViewById(R.id.user_id);

        assertEquals(R.id.mode_slave, mode.getCheckedButtonId());
        assertEquals(
                activity.getColor(R.color.brand_orange),
                slave.getBackgroundTintList().getColorForState(
                        new int[]{android.R.attr.state_checked}, 0));
        assertEquals(
                activity.getColor(R.color.brand_black),
                master.getBackgroundTintList().getDefaultColor());
        mode.check(R.id.mode_master);

        activity.onSyncStateChanged(SyncConnectionState.WAITING_FOR_XDRIP, false);

        assertTrue(mode.isEnabled());
        assertTrue(slave.isEnabled());
        assertTrue(master.isEnabled());
        assertTrue(master.isChecked());
        long eventTime = android.os.SystemClock.uptimeMillis();
        slave.dispatchTouchEvent(MotionEvent.obtain(
                eventTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                1.0f,
                1.0f,
                0));
        slave.dispatchTouchEvent(MotionEvent.obtain(
                eventTime,
                eventTime,
                MotionEvent.ACTION_UP,
                1.0f,
                1.0f,
                0));
        assertEquals(R.id.mode_master, mode.getCheckedButtonId());
        assertEquals(
                activity.getColor(R.color.brand_orange),
                master.getBackgroundTintList().getColorForState(
                        new int[]{android.R.attr.state_enabled, android.R.attr.state_checked}, 0));
        assertEquals(
                activity.getColor(R.color.brand_black),
                master.getTextColors().getColorForState(
                        new int[]{android.R.attr.state_enabled, android.R.attr.state_checked}, 0));
        assertEquals(
                activity.getColor(R.color.brand_orange),
                slave.getTextColors().getColorForState(
                        new int[]{android.R.attr.state_enabled, -android.R.attr.state_checked}, 0));
        assertFalse(backendUrl.isEnabled());
        assertFalse(userId.isEnabled());
        assertEquals(
                activity.getString(R.string.monitoring_waiting_for_xdrip),
                ((android.widget.TextView) activity.findViewById(R.id.monitoring_status))
                        .getText().toString());
    }

    @Test
    public void selectedMasterModeIsSavedWhenMonitoringStarts() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        Intent intent = new Intent().putExtra(
                MainActivity.EXTRA_SCREEN, PhoneScreen.CONNECTION.name());
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        activity.findViewById(R.id.mode_master).performClick();
        activity.<EditText>findViewById(R.id.user_id).setText("secret");

        assertTrue(activity.findViewById(R.id.monitoring_toggle).performClick());

        assertEquals(AppMode.MASTER, new AppPreferences(application).load().orElseThrow().mode());
    }
}
