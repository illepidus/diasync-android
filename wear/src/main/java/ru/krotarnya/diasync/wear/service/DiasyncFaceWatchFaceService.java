package ru.krotarnya.diasync.wear.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.wear.watchface.CanvasType;
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
    private final BatteryStatusReceiver batteryStatusReceiver = new BatteryStatusReceiver();
    private final WatchFaceHolder watchFaceHolder = new WatchFaceHolder();

    private DiasyncDatabase db;

    @Override
    public WatchFace createWatchFace(
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository,
            @NonNull Continuation<? super WatchFace> continuation)
    {
        DiasyncRenderer renderer = new DiasyncRenderer(
                watchFaceHolder,
                surfaceHolder,
                currentUserStyleRepository,
                watchState,
                CanvasType.HARDWARE,
                1000L);

        return new WatchFace(WatchFaceType.DIGITAL, renderer);
    }

    @Override
    public void onCreate() {
        Context context = getApplicationContext();

        db = DiasyncDatabase.getInstance(context);
        batteryStatusReceiver.register(context);

        ContextCompat.startForegroundService(context, new Intent(this, DataSyncService.class));
        EventBus.getDefault().register(this);
        
        updateSettings();
        new StepListener(context).start();
    }

    @Override
    public void onDestroy() {
        batteryStatusReceiver.unregister(getApplicationContext());
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
        Settings settings = watchFaceHolder.get().getSettings();

        Instant to = Instant.now();
        Instant from = to.minus(settings.getWatchFaceTimeWindow());
        List<DataPoint> dataPoints = db.dataPointDao().find(settings.getUserId(), from, to);

        watchFaceHolder.mutate().dataPoints(dataPoints);
    }

    private void updateSettings() {
        watchFaceHolder.mutate().settings(db.settingsDao().get());
        updateBloodData();
    }


    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onBatteryChange(BatteryStatusChanged event) {
        watchFaceHolder.mutate().batteryStatus(event.getBatteryStatus());
    }
}