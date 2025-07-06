package ru.krotarnya.diasync.wear.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.WatchFace;
import androidx.wear.watchface.WatchFaceService;
import androidx.wear.watchface.WatchFaceType;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.coroutines.Continuation;
import ru.krotarnya.diasync.common.events.BatteryStatusChanged;
import ru.krotarnya.diasync.common.events.DailyStepsUpdated;
import ru.krotarnya.diasync.common.events.NewData;
import ru.krotarnya.diasync.common.events.SettingsChanged;
import ru.krotarnya.diasync.common.receiver.BatteryStatusReceiver;
import ru.krotarnya.diasync.common.repository.DataPoint;
import ru.krotarnya.diasync.common.repository.DiasyncDatabase;
import ru.krotarnya.diasync.common.repository.Settings;
import ru.krotarnya.diasync.common.service.DataSyncService;
import ru.krotarnya.diasync.wear.render.DiasyncRenderer;

@SuppressLint("Deprecated")
public final class DiasyncFaceWatchFaceService extends WatchFaceService {
    public static final String TAG = DiasyncFaceWatchFaceService.class.getSimpleName();
    private final BatteryStatusReceiver batteryStatusReceiver = new BatteryStatusReceiver();
    private final WatchFaceHolder watchFaceHolder = new WatchFaceHolder();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private DiasyncDatabase db;

    @Override
    public WatchFace createWatchFace(
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager,
            @NonNull CurrentUserStyleRepository userStyleRepository,
            @NonNull Continuation<? super WatchFace> continuation)
    {
        DiasyncRenderer renderer = new DiasyncRenderer(watchFaceHolder, surfaceHolder, userStyleRepository, watchState);
        return new WatchFace(WatchFaceType.DIGITAL, renderer);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating");

        EventBus.getDefault().register(this);

        Context context = getApplicationContext();
        db = DiasyncDatabase.getInstance(context);
        batteryStatusReceiver.register(context);
        ContextCompat.startForegroundService(context, new Intent(context, DataSyncService.class));
        new StepListener(context).start();

        executor.submit(this::initialize);

        Log.d(TAG, "Created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroyed");

        batteryStatusReceiver.unregister(getApplicationContext());
        executor.shutdownNow();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNewData(NewData event) {
        updateBloodData();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSettings(SettingsChanged event) {
        watchFaceHolder.mutate().settings(event.getSettings());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSteps(DailyStepsUpdated event) {
        watchFaceHolder.mutate().stepsToday(event.getSteps());
    }

    private void updateBloodData() {
        Settings settings = watchFaceHolder.build().getSettings();

        Instant to = Instant.now();
        Instant from = to.minus(settings.getWatchFaceTimeWindow());
        List<DataPoint> dataPoints = db.dataPointDao().find(settings.getUserId(), from, to);

        watchFaceHolder.mutate().dataPoints(dataPoints);
    }

    private void initialize() {
        watchFaceHolder.mutate().settings(db.settingsDao().get());
        updateBloodData();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onBatteryChange(BatteryStatusChanged event) {
        watchFaceHolder.mutate().batteryStatus(event.getBatteryStatus());
    }
}