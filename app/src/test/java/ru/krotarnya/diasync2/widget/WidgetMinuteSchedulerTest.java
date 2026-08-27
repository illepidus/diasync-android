package ru.krotarnya.diasync2.widget;

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
public class WidgetMinuteSchedulerTest {
    @Test
    public void schedulesExplicitNonWakeupMinuteTick() {
        Application application = RuntimeEnvironment.getApplication();
        AlarmManager alarmManager = application.getSystemService(AlarmManager.class);
        long earliestExpected = SystemClock.elapsedRealtime() + 60_000L;

        new WidgetMinuteScheduler().schedule(application);

        ShadowAlarmManager.ScheduledAlarm alarm = shadowOf(alarmManager).getNextScheduledAlarm();
        Intent intent = shadowOf(alarm.operation).getSavedIntent();
        assertEquals(AlarmManager.ELAPSED_REALTIME, alarm.type);
        assertTrue(alarm.triggerAtTime >= earliestExpected);
        assertEquals(WidgetMinuteScheduler.ACTION_MINUTE_TICK, intent.getAction());
        assertEquals(
                new ComponentName(application, DiasyncWidgetProvider.class),
                intent.getComponent());
    }
}
