package ru.krotarnya.diasync2.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import java.time.Duration;

final class WidgetMinuteScheduler {
    static final String ACTION_MINUTE_TICK = "ru.krotarnya.diasync2.action.WIDGET_MINUTE_TICK";
    private static final Duration TICK_INTERVAL = Duration.ofMinutes(1);
    private static final int REQUEST_CODE = 1;

    void schedule(Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        alarmManager.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + TICK_INTERVAL.toMillis(),
                pendingIntent(context));
    }

    void cancel(Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        alarmManager.cancel(pendingIntent(context));
    }

    private PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, DiasyncWidgetProvider.class)
                .setAction(ACTION_MINUTE_TICK);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
