package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class SharedPreferencesWearAlertStateStoreTest {
    @Test
    public void stateSurvivesStoreRecreationInDeviceProtectedPreferences() {
        Application application = RuntimeEnvironment.getApplication();
        application.createDeviceProtectedStorageContext()
                .getSharedPreferences("wear_alert_state", 0)
                .edit()
                .clear()
                .commit();
        Instant noDataAt = Instant.parse("2026-08-29T12:00:00Z");
        SharedPreferencesWearAlertStateStore first =
                new SharedPreferencesWearAlertStateStore(application);

        assertTrue(first.write(new WearAlertState("LOW:stable", noDataAt, WearDataPhase.NO_DATA)));
        WearAlertState restored = new SharedPreferencesWearAlertStateStore(application).read();

        assertEquals("LOW:stable", restored.lastProcessedEventId());
        assertEquals(noDataAt, restored.lastNoDataAlertAt());
        assertEquals(WearDataPhase.NO_DATA, restored.dataPhase());
    }
}
