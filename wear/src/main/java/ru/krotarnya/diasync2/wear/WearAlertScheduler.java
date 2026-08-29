package ru.krotarnya.diasync2.wear;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.time.Instant;
import java.util.Objects;

final class WearAlertScheduler {
    static final String ACTION_CHECK = "ru.krotarnya.diasync2.action.WEAR_ALERT_CHECK";
    private static final int REQUEST_CODE = 9;

    void schedule(Context context, Instant checkAt) {
        Objects.requireNonNull(checkAt);
        long triggerAtMillis = Math.max(
                checkAt.toEpochMilli(),
                System.currentTimeMillis() + 1_000L);
        context.getSystemService(AlarmManager.class).setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent(context));
    }

    void cancel(Context context) {
        context.getSystemService(AlarmManager.class).cancel(pendingIntent(context));
    }

    private PendingIntent pendingIntent(Context context) {
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                new Intent(context, WearAlertReceiver.class).setAction(ACTION_CHECK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
