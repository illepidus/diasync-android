package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WearAlertSchedulerTest {
    @Test
    public void schedulesExplicitWakeupCheckAtRequestedWallClockTime() {
        Application application = RuntimeEnvironment.getApplication();
        Instant checkAt = Instant.ofEpochMilli(System.currentTimeMillis() + 120_000L);

        new WearAlertScheduler().schedule(application, checkAt);

        AlarmManager manager = application.getSystemService(AlarmManager.class);
        ShadowAlarmManager.ScheduledAlarm alarm = shadowOf(manager).getNextScheduledAlarm();
        Intent intent = shadowOf(alarm.operation).getSavedIntent();
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type);
        assertEquals(checkAt.toEpochMilli(), alarm.triggerAtTime);
        assertEquals(WearAlertScheduler.ACTION_CHECK, intent.getAction());
        assertEquals(new ComponentName(application, WearAlertReceiver.class), intent.getComponent());
    }
}
