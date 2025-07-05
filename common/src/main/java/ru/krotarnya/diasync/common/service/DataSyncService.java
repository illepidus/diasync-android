package ru.krotarnya.diasync.common.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.krotarnya.diasync.common.api.DiasyncApiService;
import ru.krotarnya.diasync.common.api.InstantTypeAdapter;
import ru.krotarnya.diasync.common.repository.Database;

public class DataSyncService extends Service {
    private static final String TAG = DataSyncService.class.getSimpleName();
    
    private Database db;
    private DiasyncApiService api;
    private ScheduledExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        db = Database.cons(getApplicationContext());

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://diasync.krotarnya.ru")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        api = retrofit.create(DiasyncApiService.class);

        startForeground(1, buildNotification());
        Log.d(TAG, "Foreground service started with notification");

        executorService = Executors.newSingleThreadScheduledExecutor();
        startSync(this::getUserId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void startSync(Supplier<String> userIdSupplier) {
        executorService.scheduleWithFixedDelay(
                new DataSyncTask(userIdSupplier, db, api),
                0,
                5,
                TimeUnit.SECONDS);
    }

    private Notification buildNotification() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        String channelId = "sync_channel";
        String channelName = "Data Sync";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Channel for data synchronization notifications");
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(this, channelId)
                .setContentTitle("Diasync sync service")
                .setContentText("Synchronizing...")
                .setOngoing(true);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String getUserId() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("user_id", "demo");
    }
}