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
        List<String> hiddenNotifications = new ArrayList<>();
        List<String> events = new ArrayList<>();
        PhoneAlertController controller = controller(
                List.of(point(60.0, NOW), point(65.0, NOW.minusSeconds(60))),
                sounds,
                notifications,
                hiddenNotifications,
                (type, timestamp) -> events.add(type + ":" + timestamp));

        controller.checkAsync();

        assertEquals(List.of(AlertType.LOW), sounds);
        assertEquals(List.of(AlertType.LOW), notifications);
        assertEquals(List.of("HIGH", "NO_DATA"), hiddenNotifications);
        assertEquals(List.of("LOW:" + NOW), events);
    }

    @Test
    public void noDataAlertDoesNotPublishWearEvent() {
        preferences.saveAlertSettings(new AlertSettings(false, false, true));
        List<AlertType> sounds = new ArrayList<>();
        List<AlertType> notifications = new ArrayList<>();
        List<String> hiddenNotifications = new ArrayList<>();
        List<String> events = new ArrayList<>();
        PhoneAlertController controller = controller(
                List.of(),
                sounds,
                notifications,
                hiddenNotifications,
                (type, timestamp) -> events.add(type.name()));

        controller.checkNow();

        assertEquals(List.of(AlertType.NO_DATA), sounds);
        assertEquals(List.of(AlertType.NO_DATA), notifications);
        assertEquals(List.of("LOW", "HIGH"), hiddenNotifications);
        assertTrue(events.isEmpty());
    }

    @Test
    public void phoneSnoozeDoesNotSuppressWearGlucoseAlertWhenWearSnoozeIsDisabled() {
        preferences.saveAlertSettings(new AlertSettings(true, true, true));
        preferences.snoozeUntil(NOW.plusSeconds(300));
        preferences.saveSnoozeWearAlerts(false);
        List<AlertType> sounds = new ArrayList<>();
        List<AlertType> notifications = new ArrayList<>();
        List<String> events = new ArrayList<>();
        PhoneAlertController controller = controller(
                List.of(point(60.0, NOW), point(65.0, NOW.minusSeconds(60))),
                sounds,
                notifications,
                new ArrayList<>(),
                (type, timestamp) -> events.add(type + ":" + timestamp));

        controller.checkNow();

        assertTrue(sounds.isEmpty());
        assertTrue(notifications.isEmpty());
        assertEquals(List.of("LOW:" + NOW), events);
    }

    @Test
    public void phoneSnoozeDoesNotSuppressWearHighAlertWhenWearSnoozeIsDisabled() {
        preferences.saveAlertSettings(new AlertSettings(true, true, true));
        preferences.snoozeUntil(NOW.plusSeconds(300));
        preferences.saveSnoozeWearAlerts(false);
        List<AlertType> sounds = new ArrayList<>();
        List<AlertType> notifications = new ArrayList<>();
        List<String> events = new ArrayList<>();
        PhoneAlertController controller = controller(
                List.of(point(190.0, NOW), point(185.0, NOW.minusSeconds(60))),
                sounds,
                notifications,
                new ArrayList<>(),
                (type, timestamp) -> events.add(type + ":" + timestamp));

        controller.checkNow();

        assertTrue(sounds.isEmpty());
        assertTrue(notifications.isEmpty());
        assertEquals(List.of("HIGH:" + NOW), events);
    }

    @Test
    public void phoneSnoozeSuppressesWearGlucoseAlertWhenWearSnoozeIsEnabled() {
        preferences.saveAlertSettings(new AlertSettings(true, true, true));
        preferences.snoozeUntil(NOW.plusSeconds(300));
        List<AlertType> sounds = new ArrayList<>();
        List<AlertType> notifications = new ArrayList<>();
        List<String> events = new ArrayList<>();
        PhoneAlertController controller = controller(
                List.of(point(60.0, NOW), point(65.0, NOW.minusSeconds(60))),
                sounds,
                notifications,
                new ArrayList<>(),
                (type, timestamp) -> events.add(type + ":" + timestamp));

        controller.checkNow();

        assertTrue(sounds.isEmpty());
        assertTrue(notifications.isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test
    public void phoneSnoozeDoesNotEvaluateNoDataForWearBecauseWatchOwnsIt() {
        preferences.saveAlertSettings(new AlertSettings(false, false, true));
        preferences.snoozeUntil(NOW.plusSeconds(300));
        preferences.saveSnoozeWearAlerts(false);
        PhoneAlertController controller = controller(
                List.of(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                (type, timestamp) -> { });

        controller.checkNow();

        assertEquals(Instant.EPOCH, preferences.lastAlertAt());
    }

    @Test
    public void normalReadingHidesPreviousGlucoseNotification() {
        preferences.saveAlertSettings(new AlertSettings(true, true, true));
        List<String> hiddenNotifications = new ArrayList<>();
        PhoneAlertController controller = controller(
                List.of(point(110.0, NOW), point(100.0, NOW.minusSeconds(60))),
                new ArrayList<>(),
                new ArrayList<>(),
                hiddenNotifications,
                (type, timestamp) -> { });

        controller.checkNow();

        assertEquals(List.of("LOW", "HIGH", "NO_DATA"), hiddenNotifications);
    }

    @Test
    public void lowReadingKeepsNotificationWhenRepeatIsNotDue() {
        preferences.saveAlertSettings(new AlertSettings(true, true, true));
        List<String> hiddenNotifications = new ArrayList<>();
        PhoneAlertController controller = controller(
                List.of(point(60.0, NOW), point(55.0, NOW.minusSeconds(60))),
                new ArrayList<>(),
                new ArrayList<>(),
                hiddenNotifications,
                (type, timestamp) -> { });

        controller.checkNow();

        assertEquals(List.of("HIGH", "NO_DATA"), hiddenNotifications);
    }

    private PhoneAlertController controller(
            List<DataPoint> points,
            List<AlertType> sounds,
            List<AlertType> notifications,
            List<String> hiddenNotifications,
            AlertEventOutput output
    ) {
        return new PhoneAlertController(
                preferences,
                (userId, limit) -> points,
                new AlertEvaluator(Clock.fixed(NOW, ZoneOffset.UTC)),
                sounds::add,
                notifications::add,
                type -> hiddenNotifications.add(type.name()),
                output,
                Runnable::run,
                Clock.fixed(NOW, ZoneOffset.UTC));
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
