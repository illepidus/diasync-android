package ru.krotarnya.diasync2.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import ru.krotarnya.diasync2.common.AlertEvaluator;
import ru.krotarnya.diasync2.common.AlertType;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.GlucoseValue;
import ru.krotarnya.diasync2.common.SensorPoint;
import ru.krotarnya.diasync2.settings.AlertSettings;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.settings.GraphWindow;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class PhoneAlertControllerTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

    private AppPreferences preferences;

    @Before
    public void setUp() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        preferences = new AppPreferences(application);
        preferences.save(new AppConfiguration(
                "https://example.test",
                "secret",
                GlucoseUnit.MMOL_L,
                true,
                70.0,
                180.0,
                GraphWindow.THIRTY_MINUTES,
                true,
                false,
                true));
    }

    @Test
    public void glucoseAlertPlaysNotifiesAndPublishesWearHook() {
        preferences.saveAlertSettings(new AlertSettings(true, true, true));
        List<AlertType> sounds = new ArrayList<>();
        List<AlertType> notifications = new ArrayList<>();
        List<String> events = new ArrayList<>();
        PhoneAlertController controller = controller(
                List.of(point(60.0, NOW), point(65.0, NOW.minusSeconds(60))),
                sounds,
                notifications,
                (type, timestamp) -> events.add(type + ":" + timestamp));

        controller.checkAsync();

        assertEquals(List.of(AlertType.LOW), sounds);
        assertEquals(List.of(AlertType.LOW), notifications);
        assertEquals(List.of("LOW:" + NOW), events);
    }

    @Test
    public void noDataAlertDoesNotPublishWearEvent() {
        preferences.saveAlertSettings(new AlertSettings(false, false, true));
        List<AlertType> sounds = new ArrayList<>();
        List<AlertType> notifications = new ArrayList<>();
        List<String> events = new ArrayList<>();
        PhoneAlertController controller = controller(
                List.of(),
                sounds,
                notifications,
                (type, timestamp) -> events.add(type.name()));

        controller.checkNow();

        assertEquals(List.of(AlertType.NO_DATA), sounds);
        assertEquals(List.of(AlertType.NO_DATA), notifications);
        assertTrue(events.isEmpty());
    }

    private PhoneAlertController controller(
            List<DataPoint> points,
            List<AlertType> sounds,
            List<AlertType> notifications,
            AlertEventOutput output
    ) {
        return new PhoneAlertController(
                preferences,
                (userId, limit) -> points,
                new AlertEvaluator(Clock.fixed(NOW, ZoneOffset.UTC)),
                sounds::add,
                notifications::add,
                output,
                Runnable::run);
    }

    private DataPoint point(double mgDl, Instant timestamp) {
        return new DataPoint(
                timestamp,
                timestamp,
                new SensorPoint(timestamp, new GlucoseValue(mgDl), "sensor", null),
                null,
                null,
                null);
    }
}
