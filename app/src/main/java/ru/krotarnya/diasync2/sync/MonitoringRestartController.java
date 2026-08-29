package ru.krotarnya.diasync2.sync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Objects;
import ru.krotarnya.diasync2.MainActivity;
import ru.krotarnya.diasync2.R;
import ru.krotarnya.diasync2.settings.AppPreferences;

public final class MonitoringRestartController {
    static final String BLOCKED_CHANNEL_ID = "monitoring_restart";
    static final int BLOCKED_NOTIFICATION_ID = 1002;

    interface Starter {
        void start(Context context);
    }

    interface BlockedNotifier {
        void show(Context context);

        void cancel(Context context);
    }

    enum Result {
        NOT_ENABLED,
        STARTED,
        BLOCKED
    }

    private MonitoringRestartController() {
    }

    public static void restart(Context context) {
        Context applicationContext = context.getApplicationContext();
        restart(
                applicationContext,
                new AppPreferences(applicationContext),
                MonitoringService::start,
                new NotificationBlockedNotifier());
    }

    static Result restart(
            Context context,
            AppPreferences preferences,
            Starter starter,
            BlockedNotifier notifier
    ) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(preferences);
        Objects.requireNonNull(starter);
        Objects.requireNonNull(notifier);
        if (!preferences.monitoringEnabled() || preferences.load().isEmpty()) {
            return Result.NOT_ENABLED;
        }
        preferences.saveSyncConnectionState(SyncConnectionState.CONNECTING);
        try {
            starter.start(context);
            notifier.cancel(context);
            return Result.STARTED;
        } catch (RuntimeException exception) {
            if (!isBackgroundStartBlocked(exception)) {
                throw exception;
            }
            preferences.saveSyncConnectionState(SyncConnectionState.BLOCKED);
            notifier.show(context);
            return Result.BLOCKED;
        }
    }

    static void cancelBlockedNotification(Context context) {
        context.getSystemService(NotificationManager.class).cancel(BLOCKED_NOTIFICATION_ID);
    }

    private static boolean isBackgroundStartBlocked(RuntimeException exception) {
        return exception instanceof IllegalStateException
                || exception instanceof SecurityException;
    }

    private static final class NotificationBlockedNotifier implements BlockedNotifier {
        @Override
        public void show(Context context) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(
                    BLOCKED_CHANNEL_ID,
                    context.getString(R.string.monitoring_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
            PendingIntent open = PendingIntent.getActivity(
                    context,
                    2,
                    new Intent(context, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification notification = new Notification.Builder(context, BLOCKED_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(context.getString(
                            R.string.monitoring_restart_required_title))
                    .setContentText(context.getString(
                            R.string.monitoring_restart_required_text))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build();
            manager.notify(BLOCKED_NOTIFICATION_ID, notification);
        }

        @Override
        public void cancel(Context context) {
            cancelBlockedNotification(context);
        }
    }
}
