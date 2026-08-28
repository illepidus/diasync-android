package ru.krotarnya.diasync2.alert;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import ru.krotarnya.diasync2.MainActivity;
import ru.krotarnya.diasync2.R;
import ru.krotarnya.diasync2.common.AlertType;

public final class AlertNotificationPublisher {
    static final String CHANNEL_ID = "glucose_alerts";
    static final int NOTIFICATION_ID = 2001;

    private final Context context;
    private final NotificationManager notificationManager;

    public AlertNotificationPublisher(Context context) {
        this.context = context.getApplicationContext();
        notificationManager = context.getSystemService(NotificationManager.class);
        createChannel();
    }

    public void show(AlertType type) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(title(type)))
                .setContentText(context.getString(R.string.alert_notification_text))
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.alert_channel_description));
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
    }

    private int title(AlertType type) {
        return switch (type) {
            case LOW -> R.string.alert_low_title;
            case HIGH -> R.string.alert_high_title;
            case NO_DATA -> R.string.alert_no_data_title;
        };
    }
}
