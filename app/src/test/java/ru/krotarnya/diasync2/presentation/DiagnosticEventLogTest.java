package ru.krotarnya.diasync2.presentation;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.Context;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class DiagnosticEventLogTest {
    @Test
    public void hidesPreviouslyStoredMasterLoopStateNoise() {
        Application application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences("diagnostic_events", Context.MODE_PRIVATE)
                .edit().clear().commit();
        DiagnosticEventLog eventLog = new DiagnosticEventLog(application);
        Instant now = Instant.parse("2026-09-12T17:00:00Z");

        eventLog.record("Sync", "WAITING_FOR_XDRIP", now);
        eventLog.record("Sync", "UPLOADING", now.plusSeconds(1));
        eventLog.record("Master upload", "UPLOAD_SUCCEEDED", now.plusSeconds(2));

        assertEquals(
                List.of("2026-09-12T17:00:02Z · Master upload · UPLOAD_SUCCEEDED"),
                eventLog.latest());
    }
}
