package ru.krotarnya.diasync.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.room.Room;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.krotarnya.diasync.app.R;
import ru.krotarnya.diasync.app.api.ApiService;
import ru.krotarnya.diasync.app.api.InstantTypeAdapter;
import ru.krotarnya.diasync.app.repository.AppDatabase;

/**
 * @noinspection NullableProblems
 */
public class SyncService extends Service {
    private AppDatabase db;
    private ApiService api;
    private ScheduledExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SyncService", "Service created");

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "diasync-db")
                .fallbackToDestructiveMigration()
                .build();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://diasync.krotarnya.ru")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        api = retrofit.create(ApiService.class);

        startForeground(1, buildNotification());
        Log.d("SyncService", "Foreground service started with notification");

        executorService = Executors.newSingleThreadScheduledExecutor();
        startSync();
    }

    private void startSync() {
        executorService.scheduleWithFixedDelay(
                new DataSyncTask("demo", db, api),
                0,
                10,
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
                .setSmallIcon(R.mipmap.ic_launcher)
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
}