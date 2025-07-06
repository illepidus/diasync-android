package ru.krotarnya.diasync.wear.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.PassiveListenerCallback;
import androidx.health.services.client.PassiveMonitoringClient;
import androidx.health.services.client.data.DataPointContainer;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.IntervalDataPoint;
import androidx.health.services.client.data.PassiveListenerConfig;

import java.util.List;
import java.util.Set;

import ru.krotarnya.diasync.common.events.DailyStepsUpdated;

public class StepListener {
    private static final String TAG = StepListener.class.getSimpleName();
    private static final PassiveListenerConfig config = new PassiveListenerConfig.Builder()
            .setDataTypes(Set.of(DataType.STEPS_DAILY))
            .build();

    private final Context context;
    private final PassiveMonitoringClient client;

    public StepListener(Context context) {
        this.context = context;
        this.client = HealthServices.getClient(context).getPassiveMonitoringClient();
    }

    public void start() {
        requestPermissionsIfNeeded();
        client.setPassiveListenerCallback(config, new Callback());
    }

    @SuppressLint("WearRecents")
    private void requestPermissionsIfNeeded() {
        if (hasActivityRecognitionPermission()) return;

        try {
            Toast.makeText(context, "Enable Permissions → Physical activity", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Was not able to create toast with permissions instructions");
        }
    }

    private boolean hasActivityRecognitionPermission() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACTIVITY_RECOGNITION);
    }

    private static class Callback implements PassiveListenerCallback {
        @Override
        public void onNewDataPointsReceived(@NonNull DataPointContainer container) {
            List<IntervalDataPoint<Long>> stepPoints = container.getData(DataType.STEPS_DAILY);

            if (!stepPoints.isEmpty()) {
                IntervalDataPoint<Long> latest = stepPoints.get(stepPoints.size() - 1);
                long steps = latest.getValue();
                new DailyStepsUpdated(steps).post();
                Log.d("StepListener", "Steps today: " + steps);
            } else {
                Log.w("StepListener", "No new data");
            }
        }

        @Override
        public void onRegistered() {
            Log.d("StepListener", "Registered");
        }

        @Override
        public void onRegistrationFailed(@NonNull Throwable throwable) {
            Log.e("StepListener", "Registration error", throwable);
        }
    }
}
