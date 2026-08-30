package ru.krotarnya.diasync2.alert;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import ru.krotarnya.diasync2.R;
import ru.krotarnya.diasync2.common.AlertType;

public final class AlertSoundPlayer {
    interface Player {
        void setOnCompletionListener(Runnable listener);

        void setOnErrorListener(Runnable listener);

        void start();

        void release();
    }

    @FunctionalInterface
    interface PlayerFactory {
        Player create(Context context, int resourceId, AudioAttributes attributes);
    }

    private final Context context;
    private final PlayerFactory playerFactory;
    private final AudioAttributes audioAttributes;
    private final Set<Player> activePlayers =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public AlertSoundPlayer(Context context) {
        this(context.getApplicationContext(), AlertSoundPlayer::createMediaPlayer);
    }

    AlertSoundPlayer(Context context, PlayerFactory playerFactory) {
        this.context = Objects.requireNonNull(context);
        this.playerFactory = Objects.requireNonNull(playerFactory);
        this.audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    public void play(AlertType type) {
        Player player = playerFactory.create(context, resource(type), audioAttributes);
        if (player == null) {
            return;
        }
        retain(player);
        AtomicBoolean released = new AtomicBoolean();
        Runnable release = () -> {
            if (released.compareAndSet(false, true)) {
                forget(player);
                player.release();
            }
        };
        try {
            player.setOnCompletionListener(release);
            player.setOnErrorListener(release);
            player.start();
        } catch (RuntimeException exception) {
            release.run();
        }
    }

    synchronized int activePlayerCount() {
        return activePlayers.size();
    }

    private synchronized void retain(Player player) {
        activePlayers.add(player);
    }

    private synchronized void forget(Player player) {
        activePlayers.remove(player);
    }

    private int resource(AlertType type) {
        return switch (type) {
            case LOW -> R.raw.alert_low;
            case HIGH -> R.raw.alert_high;
            case NO_DATA -> R.raw.alert_no_data;
        };
    }

    private static Player createMediaPlayer(
            Context context,
            int resourceId,
            AudioAttributes attributes
    ) {
        MediaPlayer mediaPlayer = MediaPlayer.create(context, resourceId, attributes, 0);
        return mediaPlayer == null ? null : new Player() {
            @Override
            public void setOnCompletionListener(Runnable listener) {
                mediaPlayer.setOnCompletionListener(ignored -> listener.run());
            }

            @Override
            public void setOnErrorListener(Runnable listener) {
                mediaPlayer.setOnErrorListener((ignored, what, extra) -> {
                    listener.run();
                    return true;
                });
            }

            @Override
            public void start() {
                mediaPlayer.start();
            }

            @Override
            public void release() {
                mediaPlayer.release();
            }
        };
    }
}
