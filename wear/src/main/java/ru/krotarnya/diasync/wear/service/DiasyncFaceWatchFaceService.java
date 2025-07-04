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

import java.util.List;
import java.util.stream.Collectors;

import kotlin.coroutines.Continuation;
import ru.krotarnya.diasync.common.events.BatteryStatusEvent;
import ru.krotarnya.diasync.common.events.NewDataEvent;
import ru.krotarnya.diasync.common.receiver.BatteryStatusReceiver;
import ru.krotarnya.diasync.common.repository.DataPoint;
import ru.krotarnya.diasync.common.repository.Database;
import ru.krotarnya.diasync.common.service.DataSyncService;
import ru.krotarnya.diasync.wear.render.DiasyncRenderer;

@SuppressLint("Deprecated")
public final class DiasyncFaceWatchFaceService extends WatchFaceService {
    private final BatteryStatusReceiver batteryStatusReceiver = new BatteryStatusReceiver();
    private final WatchDataHolder watchDataHolder = new WatchDataHolder();

    private Database database;

    @Override
    public WatchFace createWatchFace(
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository,
            @NonNull Continuation<? super WatchFace> continuation)
    {
        DiasyncRenderer renderer = new DiasyncRenderer(
                watchDataHolder,
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

        database = Database.cons(context);
        batteryStatusReceiver.register(context);
        ContextCompat.startForegroundService(context, new Intent(this, DataSyncService.class));

        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        batteryStatusReceiver.unregister(getApplicationContext());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNewData(NewDataEvent event) {
        updateBloodData();
    }

    private void updateBloodData() {
        List<DataPoint> dataPoints = database.dataPointDao()
                .findLast("krotarino")
                .stream()
                .collect(Collectors.toList());

        watchDataHolder.mutate().dataPoints(dataPoints);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onBatteryChange(BatteryStatusEvent event) {
        watchDataHolder.mutate().batteryStatus(event.getBatteryStatus());
    }
}