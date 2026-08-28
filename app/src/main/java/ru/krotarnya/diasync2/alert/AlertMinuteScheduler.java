package ru.krotarnya.diasync2.alert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import java.time.Duration;

public final class AlertMinuteScheduler {
    public static final String ACTION_MINUTE_TICK =
            "ru.krotarnya.diasync2.action.ALERT_MINUTE_TICK";
    private static final Duration TICK_INTERVAL = Duration.ofMinutes(1);
    private static final int REQUEST_CODE = 3;

    public void schedule(Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        alarmManager.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + TICK_INTERVAL.toMillis(),
                pendingIntent(context));
    }

    public void cancel(Context context) {
        context.getSystemService(AlarmManager.class).cancel(pendingIntent(context));
    }

    private PendingIntent pendingIntent(Context context) {
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                new Intent(context, AlertMinuteReceiver.class).setAction(ACTION_MINUTE_TICK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
