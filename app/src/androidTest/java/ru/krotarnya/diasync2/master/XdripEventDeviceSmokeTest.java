package ru.krotarnya.diasync2.master;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.krotarnya.diasync2.DiasyncApplication;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AppMode;
import ru.krotarnya.diasync2.settings.GraphWindow;

@RunWith(AndroidJUnit4.class)
public class XdripEventDeviceSmokeTest {
    @Test
    public void signedFixtureBroadcastIsDurablyAccepted() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DiasyncApplication application = (DiasyncApplication) target.getApplicationContext();
        application.preferences().save(new AppConfiguration(
                AppMode.MASTER,
                "https://example.test/",
                "device-smoke-user",
                GlucoseUnit.MMOL_L,
                true,
                70.0,
                180.0,
                GraphWindow.THIRTY_MINUTES,
                true,
                false,
                true));
        application.preferences().setMonitoringEnabled(true);
        String eventId = "SENSOR:" + UUID.randomUUID();
        String payload = "{\"protocolVersion\":1,"
                + "\"eventId\":\"" + eventId + "\","
                + "\"eventType\":\"SENSOR\","
                + "\"occurredAtEpochMillis\":1789027200000,"
                + "\"sensor\":{\"rawValue\":123.0,\"sensorId\":\"device-smoke\","
                + "\"calibration\":{\"slope\":1.0,\"intercept\":0.0}}}";

        Intent intent = new Intent(XdripEventReceiver.ACTION_XDRIP_EVENT)
                .setComponent(new ComponentName(target, XdripEventReceiver.class))
                .putExtra(XdripEventReceiver.EXTRA_PAYLOAD, payload);
        target.sendBroadcast(intent);

        long deadline = System.nanoTime() + 5_000_000_000L;
        while (application.masterEventIngestor() != null
                && System.nanoTime() < deadline
                && application.masterEventDao().findEvent(eventId) == null) {
            Thread.sleep(25L);
        }
        assertTrue(application.masterEventDao().findEvent(eventId) != null);
        assertEquals(
                MasterEventState.PENDING.name(),
                application.masterEventDao().findEvent(eventId).state);
    }
}
