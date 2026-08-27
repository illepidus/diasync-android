package ru.krotarnya.diasync2.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import ru.krotarnya.diasync2.common.GlucoseUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AppPreferencesTest {
    @Test
    public void persistsWidgetSettings() {
        Application application = RuntimeEnvironment.getApplication();
        AppPreferences preferences = new AppPreferences(application);
        AppConfiguration expected = new AppConfiguration(
                "https://example.test",
                "secret",
                GlucoseUnit.MG_DL,
                false,
                75.0,
                170.0,
                GraphWindow.THREE_HOURS,
                false,
                true,
                false);

        preferences.save(expected);
        Optional<AppConfiguration> loaded = preferences.load();

        assertTrue(loaded.isPresent());
        assertEquals(75.0, loaded.get().lowMgDl(), 0.01);
        assertEquals(170.0, loaded.get().highMgDl(), 0.01);
        assertEquals(GraphWindow.THREE_HOURS, loaded.get().widgetGraphWindow());
        assertFalse(loaded.get().widgetGraphZones());
        assertTrue(loaded.get().widgetGraphLines());
        assertFalse(loaded.get().widgetTrendArrow());
    }

    @Test
    public void persistsWidgetSettingsWithoutCredentials() {
        Application application = RuntimeEnvironment.getApplication();
        AppPreferences preferences = new AppPreferences(application);
        WidgetSettings expected = new WidgetSettings(
                GlucoseUnit.MG_DL,
                false,
                72.0,
                181.0,
                GraphWindow.ONE_HOUR,
                false,
                true,
                false);

        preferences.saveWidgetSettings(expected);

        assertTrue(preferences.load().isEmpty());
        assertEquals(expected, preferences.loadWidgetSettings());
    }
}
