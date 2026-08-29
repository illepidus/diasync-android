package ru.krotarnya.diasync2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import android.content.Intent;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import ru.krotarnya.diasync2.navigation.PhoneScreen;
import ru.krotarnya.diasync2.presentation.StatusState;

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
}
