package ru.krotarnya.diasync2.alert;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.media.AudioAttributes;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import ru.krotarnya.diasync2.R;
import ru.krotarnya.diasync2.common.AlertType;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AlertSoundPlayerTest {
    @Test
    public void everyTypeUsesBundledResourceAndAlarmUsage() {
        Application application = RuntimeEnvironment.getApplication();
        Map<AlertType, Integer> expected = new EnumMap<>(AlertType.class);
        expected.put(AlertType.LOW, R.raw.alert_low);
        expected.put(AlertType.HIGH, R.raw.alert_high);
        expected.put(AlertType.NO_DATA, R.raw.alert_no_data);

        for (Map.Entry<AlertType, Integer> entry : expected.entrySet()) {
            RecordingFactory factory = new RecordingFactory(new FakePlayer());
            new AlertSoundPlayer(application, factory).play(entry.getKey());
            assertEquals(entry.getValue().intValue(), factory.resourceId);
            assertEquals(AudioAttributes.USAGE_ALARM, factory.usage);
        }
    }

    @Test
    public void releasesPlayerOnceAfterCompletionOrError() {
        Application application = RuntimeEnvironment.getApplication();
        FakePlayer completed = new FakePlayer();
        new AlertSoundPlayer(application, new RecordingFactory(completed)).play(AlertType.LOW);
        completed.completion.run();
        completed.error.run();
        assertEquals(1, completed.releaseCount);

        FakePlayer failed = new FakePlayer();
        new AlertSoundPlayer(application, new RecordingFactory(failed)).play(AlertType.HIGH);
        failed.error.run();
        failed.completion.run();
        assertEquals(1, failed.releaseCount);
    }

    @Test
    public void releasesPlayerWhenStartThrows() {
        Application application = RuntimeEnvironment.getApplication();
        FakePlayer player = new FakePlayer();
        player.throwOnStart = true;

        new AlertSoundPlayer(application, new RecordingFactory(player)).play(AlertType.NO_DATA);

        assertEquals(1, player.releaseCount);
    }

    private static final class RecordingFactory implements AlertSoundPlayer.PlayerFactory {
        private final FakePlayer player;
        private int resourceId;
        private int usage;

        private RecordingFactory(FakePlayer player) {
            this.player = player;
        }

        @Override
        public AlertSoundPlayer.Player create(
                android.content.Context context,
                int resourceId,
                AudioAttributes attributes
        ) {
            this.resourceId = resourceId;
            usage = attributes.getUsage();
            return player;
        }
    }

    private static final class FakePlayer implements AlertSoundPlayer.Player {
        private Runnable completion;
        private Runnable error;
        private boolean throwOnStart;
        private int releaseCount;

        @Override
        public void setOnCompletionListener(Runnable listener) {
            completion = listener;
        }

        @Override
        public void setOnErrorListener(Runnable listener) {
            error = listener;
        }

        @Override
        public void start() {
            if (throwOnStart) {
                throw new IllegalStateException("start failed");
            }
        }

        @Override
        public void release() {
            releaseCount++;
        }
    }
}
