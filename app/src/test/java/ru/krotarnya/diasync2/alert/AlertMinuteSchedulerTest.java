package ru.krotarnya.diasync2.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemClock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AlertMinuteSchedulerTest {
    @Test
    public void schedulesExplicitNonWakeupMinuteCheck() {
        Application application = RuntimeEnvironment.getApplication();
        AlarmManager manager = application.getSystemService(AlarmManager.class);
        long earliestExpected = SystemClock.elapsedRealtime() + 60_000L;

        new AlertMinuteScheduler().schedule(application);

        ShadowAlarmManager.ScheduledAlarm alarm = shadowOf(manager).getNextScheduledAlarm();
        Intent intent = shadowOf(alarm.operation).getSavedIntent();
        assertEquals(AlarmManager.ELAPSED_REALTIME, alarm.type);
        assertTrue(alarm.triggerAtTime >= earliestExpected);
        assertEquals(AlertMinuteScheduler.ACTION_MINUTE_TICK, intent.getAction());
        assertEquals(
                new ComponentName(application, AlertMinuteReceiver.class),
                intent.getComponent());
    }
}
