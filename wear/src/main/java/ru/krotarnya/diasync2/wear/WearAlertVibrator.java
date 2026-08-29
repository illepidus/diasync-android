package ru.krotarnya.diasync2.wear;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import ru.krotarnya.diasync2.common.AlertType;

final class WearAlertVibrator {
    private final Vibrator vibrator;

    WearAlertVibrator(Context context) {
        VibratorManager manager = context.getSystemService(VibratorManager.class);
        vibrator = manager == null ? null : manager.getDefaultVibrator();
    }

    void vibrate(AlertType type) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        WearVibrationCommand command = WearVibrationCommand.forAlert(type);
        vibrator.vibrate(VibrationEffect.createWaveform(
                command.timings(),
                command.amplitudes(),
                -1));
    }
}
