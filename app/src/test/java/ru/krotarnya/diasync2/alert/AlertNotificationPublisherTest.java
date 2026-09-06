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

    @Test
    public void hideGlucoseAlertKeepsNoDataNotification() {
        Application application = RuntimeEnvironment.getApplication();
        AlertNotificationPublisher publisher = new AlertNotificationPublisher(application);
        NotificationManager manager = application.getSystemService(NotificationManager.class);

        publisher.show(AlertType.LOW);
        publisher.show(AlertType.NO_DATA);
        publisher.hide(AlertType.LOW);

        assertNull(shadowOf(manager).getNotification(AlertNotificationPublisher.NOTIFICATION_ID));
        assertEquals(
                contextTitle(application, AlertType.NO_DATA),
                shadowOf(manager)
                        .getNotification(AlertNotificationPublisher.NO_DATA_NOTIFICATION_ID)
                        .extras
                        .getString(Notification.EXTRA_TITLE));
    }

    @Test
    public void hidingLowDoesNotHideHighNotification() {
        Application application = RuntimeEnvironment.getApplication();
        AlertNotificationPublisher publisher = new AlertNotificationPublisher(application);
        NotificationManager manager = application.getSystemService(NotificationManager.class);

        publisher.show(AlertType.LOW);
        publisher.show(AlertType.HIGH);
        publisher.hide(AlertType.LOW);

        assertNull(shadowOf(manager).getNotification(AlertNotificationPublisher.NOTIFICATION_ID));
        assertEquals(
                application.getString(ru.krotarnya.diasync2.R.string.alert_high_title),
                shadowOf(manager)
                        .getNotification(AlertNotificationPublisher.HIGH_NOTIFICATION_ID)
                        .extras
                        .getString(Notification.EXTRA_TITLE));
    }

    @Test
    public void hidesNoDataNotificationAfterRecovery() {
        Application application = RuntimeEnvironment.getApplication();
        AlertNotificationPublisher publisher = new AlertNotificationPublisher(application);
        NotificationManager manager = application.getSystemService(NotificationManager.class);

        publisher.show(AlertType.NO_DATA);
        publisher.hide(AlertType.NO_DATA);

        assertNull(shadowOf(manager)
                .getNotification(AlertNotificationPublisher.NO_DATA_NOTIFICATION_ID));
    }

    private String contextTitle(Application application, AlertType type) {
        return application.getString(type == AlertType.NO_DATA
                ? ru.krotarnya.diasync2.R.string.alert_no_data_title
                : ru.krotarnya.diasync2.R.string.alert_low_title);
    }
}
