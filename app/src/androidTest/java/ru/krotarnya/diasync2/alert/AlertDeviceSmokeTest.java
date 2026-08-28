package ru.krotarnya.diasync2.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.app.NotificationManager;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.krotarnya.diasync2.common.AlertType;

@RunWith(AndroidJUnit4.class)
public class AlertDeviceSmokeTest {
    @Test
    public void playsEveryBundledAlertAndPublishesOnAlertChannel() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AlertSoundPlayer soundPlayer = new AlertSoundPlayer(context);
        AlertNotificationPublisher notificationPublisher = new AlertNotificationPublisher(context);

        for (AlertType type : AlertType.values()) {
            soundPlayer.play(type);
            notificationPublisher.show(type);
            Thread.sleep(1_500L);
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        android.app.NotificationChannel channel =
                manager.getNotificationChannel(AlertNotificationPublisher.CHANNEL_ID);
        assertNotNull(channel);
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.getImportance());
        assertFalse(channel.canBypassDnd());
    }
}
