package ru.krotarnya.diasync2.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.settings.GraphWindow;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class MonitoringRestartControllerTest {
    private Application application;
    private AppPreferences preferences;
    private RecordingNotifier notifier;

    @Before
    public void setUp() {
        application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diasync_settings", 0).edit().clear().commit();
        preferences = new AppPreferences(application);
        notifier = new RecordingNotifier();
    }

    @Test
    public void enabledMonitoringRestartsAndClearsBlockedDiagnostic() {
        configureEnabledMonitoring();
        AtomicBoolean started = new AtomicBoolean();

        MonitoringRestartController.Result result = MonitoringRestartController.restart(
                application,
                preferences,
                context -> started.set(true),
                notifier);

        assertEquals(MonitoringRestartController.Result.STARTED, result);
        assertTrue(started.get());
        assertTrue(notifier.cancelled);
        assertEquals(SyncConnectionState.CONNECTING, preferences.syncConnectionState());
    }

    @Test
    public void backgroundStartRestrictionPreservesEnabledStateAndBecomesActionable() {
        configureEnabledMonitoring();

        MonitoringRestartController.Result result = MonitoringRestartController.restart(
                application,
                preferences,
                context -> { throw new IllegalStateException("background start blocked"); },
                notifier);

        assertEquals(MonitoringRestartController.Result.BLOCKED, result);
        assertTrue(preferences.monitoringEnabled());
        assertEquals(SyncConnectionState.BLOCKED, preferences.syncConnectionState());
        assertTrue(notifier.shown);
    }

    @Test
    public void disabledMonitoringDoesNotStartAfterBoot() {
        AtomicBoolean started = new AtomicBoolean();

        MonitoringRestartController.Result result = MonitoringRestartController.restart(
                application,
                preferences,
                context -> started.set(true),
                notifier);

        assertEquals(MonitoringRestartController.Result.NOT_ENABLED, result);
        assertFalse(started.get());
        assertFalse(notifier.shown);
    }

    private void configureEnabledMonitoring() {
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
        preferences.setMonitoringEnabled(true);
    }

    private static final class RecordingNotifier
            implements MonitoringRestartController.BlockedNotifier {
        private boolean shown;
        private boolean cancelled;

        @Override
        public void show(android.content.Context context) {
            shown = true;
        }

        @Override
        public void cancel(android.content.Context context) {
            cancelled = true;
        }
    }
}
