package ru.krotarnya.diasync2.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import ru.krotarnya.diasync2.MainActivity;
import ru.krotarnya.diasync2.common.AlertType;
import ru.krotarnya.diasync2.navigation.PhoneScreen;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AlertNotificationPublisherTest {
    @Test
    public void createsVisibleChannelWithoutBypassingSoundPolicy() {
        Application application = RuntimeEnvironment.getApplication();

        new AlertNotificationPublisher(application);

        NotificationChannel channel = application.getSystemService(NotificationManager.class)
                .getNotificationChannel(AlertNotificationPublisher.CHANNEL_ID);
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.getImportance());
        assertNull(channel.getSound());
    }

    @Test
    public void contentIntentOpensAlertSettings() {
        Application application = RuntimeEnvironment.getApplication();
        AlertNotificationPublisher publisher = new AlertNotificationPublisher(application);

        publisher.show(AlertType.LOW);

        NotificationManager manager = application.getSystemService(NotificationManager.class);
        Notification notification = shadowOf(manager)
                .getNotification(AlertNotificationPublisher.NOTIFICATION_ID);
        Intent intent = shadowOf(notification.contentIntent).getSavedIntent();
        assertEquals(MainActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(
                PhoneScreen.ALERTS.name(),
                intent.getStringExtra(MainActivity.EXTRA_SCREEN));
    }
}
