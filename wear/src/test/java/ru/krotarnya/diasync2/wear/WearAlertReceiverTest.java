package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.Application;
import android.content.Intent;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;
import ru.krotarnya.diasync2.common.wear.WearSnapshotCodec;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WearAlertReceiverTest {
    @Test
    public void restoredSnapshotReschedulesNextCheckAfterReceiverStartsProcess() {
        Application application = RuntimeEnvironment.getApplication();
        Instant now = Instant.now();
        WearSnapshot snapshot = new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                now,
                List.of(new WearGlucosePoint(now.minusSeconds(10), 110.0, null, null)),
                new WearDisplayPolicy(
                        GlucoseUnit.MMOL_L, true, 70.0, 180.0, 30,
                        true, false, true, ""),
                new WearAlertPolicy(false, false, true, Instant.EPOCH),
                null);
        LastKnownWearStateRepository.create(application).replaceIfValid(
                new WearSnapshotCodec().encode(snapshot));

        new WearAlertReceiver().onReceive(
                application,
                new Intent(application, WearAlertReceiver.class)
                        .setAction(WearAlertScheduler.ACTION_CHECK));

        AlarmManager manager = application.getSystemService(AlarmManager.class);
        ShadowAlarmManager.ScheduledAlarm alarm = shadowOf(manager).getNextScheduledAlarm();
        assertNotNull(alarm);
        assertEquals(
                WearAlertScheduler.ACTION_CHECK,
                shadowOf(alarm.operation)
                        .getSavedIntent()
                        .getAction());
    }
}
