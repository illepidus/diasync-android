package ru.krotarnya.diasync.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.room.Room;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.krotarnya.diasync.app.R;
import ru.krotarnya.diasync.app.api.ApiService;
import ru.krotarnya.diasync.app.model.DataPoint;
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

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://diasync.krotarnya.ru")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(ApiService.class);

        startForeground(1, buildNotification());
        Log.d("SyncService", "Foreground service started with notification");

        executorService = Executors.newSingleThreadScheduledExecutor();
        startSync();
    }

    private void startSync() {
        executorService.scheduleWithFixedDelay(() -> {
            Log.d("SyncService", "Running");
            api.getDataPoints(
                    "demo",
                    Instant.now().minus(Duration.ofMinutes(10)),
                    Instant.now()
            ).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<List<DataPoint>> call, Response<List<DataPoint>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("SyncService", "Got " + response.body().size() + " points");
                        Executors.newSingleThreadExecutor().execute(() ->
                                db.dataPointDao().insertAll(response.body()));
                    }
                }

                @Override
                public void onFailure(Call<List<DataPoint>> call, Throwable t) {
                    Log.e("SyncService", "Failed to fetch data", t);
                }
            });
        }, 0, 10, TimeUnit.SECONDS);
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