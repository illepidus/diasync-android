package ru.krotarnya.diasync2.sync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import ru.krotarnya.diasync2.DiasyncApplication;
import ru.krotarnya.diasync2.MainActivity;
import ru.krotarnya.diasync2.R;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.presentation.StatusState;
import ru.krotarnya.diasync2.settings.AppConfiguration;

public final class MonitoringService extends Service implements SyncRunner.Listener {
    public static final String ACTION_START = "ru.krotarnya.diasync2.action.START_MONITORING";
    public static final String ACTION_STOP = "ru.krotarnya.diasync2.action.STOP_MONITORING";

    private static final String CHANNEL_ID = "monitoring";
    private static final int NOTIFICATION_ID = 1001;

    private DiasyncApplication application;
    private NotificationManager notificationManager;
    private ExecutorService executor;
    private SyncRunner runner;
    private Future<?> runnerFuture;

    public static void start(Context context) {
        context.startForegroundService(new Intent(context, MonitoringService.class)
                .setAction(ACTION_START));
    }

    public static void stop(Context context) {
        context.startService(new Intent(context, MonitoringService.class)
                .setAction(ACTION_STOP));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = (DiasyncApplication) getApplication();
        notificationManager = getSystemService(NotificationManager.class);
        executor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopMonitoring();
            return START_NOT_STICKY;
        }
        Optional<AppConfiguration> configuration = application.preferences().load();
        if (!application.preferences().monitoringEnabled() || configuration.isEmpty()) {
            stopMonitoring();
            return START_NOT_STICKY;
        }
        enterForeground(SyncConnectionState.CONNECTING);
        restartRunner(configuration.get());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        cancelRunner();
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStateChanged(SyncConnectionState state) {
        application.phoneUpdateCoordinator().stateChanged(state);
        notificationManager.notify(NOTIFICATION_ID, notification(state, true));
    }

    @Override
    public void onDataCommitted() {
        application.phoneUpdateCoordinator().dataCommitted();
        notificationManager.notify(
                NOTIFICATION_ID,
                notification(application.preferences().syncConnectionState(), true));
    }

    private void restartRunner(AppConfiguration configuration) {
        cancelRunner();
        runner = new SyncRunner(
                application.createSyncWork(configuration),
                new BackoffPolicy(Math::random),
                duration -> Thread.sleep(duration.toMillis()),
                this);
        runnerFuture = executor.submit(runner);
    }

    private void cancelRunner() {
        if (runner != null) {
            runner.stop();
            runner = null;
        }
        if (runnerFuture != null) {
            runnerFuture.cancel(true);
            runnerFuture = null;
        }
    }

    private void stopMonitoring() {
        application.preferences().setMonitoringEnabled(false);
        application.phoneUpdateCoordinator().stateChanged(SyncConnectionState.STOPPED);
        cancelRunner();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @SuppressWarnings("SameParameterValue")
    private void enterForeground(SyncConnectionState state) {
        Notification notification = notification(state, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.monitoring_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.monitoring_channel_description));
        notificationManager.createNotificationChannel(channel);
    }

    private Notification notification(SyncConnectionState state, boolean includeLatestValue) {
        Intent openIntent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                new Intent(this, MonitoringService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.monitoring_notification_title))
                .setContentText(notificationText(state, includeLatestValue))
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(
                        null,
                        getString(R.string.stop_monitoring),
                        stopPendingIntent).build())
                .build();
    }

    private String notificationText(SyncConnectionState state, boolean includeLatestValue) {
        Optional<AppConfiguration> configuration = application.preferences().load();
        String connection = getString(switch (state) {
            case STOPPED -> R.string.monitoring_stopped;
            case CONNECTING -> R.string.monitoring_connecting;
            case WAITING -> R.string.monitoring_waiting;
            case RETRYING -> R.string.monitoring_retrying;
        });
        if (!includeLatestValue || configuration.isEmpty()) {
            return connection;
        }
        DataPoint latest = application.bootstrapRepository()
                .latestLocalSensorPoint(configuration.get().userId());
        if (latest == null || latest.sensorPoint() == null) {
            return connection;
        }
        StatusState latestState = application.statusPresenter().local(
                latest,
                configuration.get().unit(),
                configuration.get().useCalibration());
        return getString(
                R.string.monitoring_notification_value,
                latestState.value(),
                latestState.unit(),
                latestState.age(),
                connection);
    }
}
