package ru.krotarnya.diasync2.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

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
}
