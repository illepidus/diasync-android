package ru.krotarnya.diasync.wear.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
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

import kotlin.coroutines.Continuation;
import ru.krotarnya.diasync.common.receiver.BatteryStatusReceiver;
import ru.krotarnya.diasync.common.repository.Database;
import ru.krotarnya.diasync.common.service.DataSyncService;
import ru.krotarnya.diasync.common.service.WatchDataProvider;
import ru.krotarnya.diasync.wear.render.DiasyncRenderer;

@SuppressLint("Deprecated")
public final class DiasyncFaceService extends WatchFaceService {
    private BatteryStatusReceiver batteryStatusReceiver;

    @Override
    public WatchFace createWatchFace(
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository,
            @NonNull Continuation<? super WatchFace> continuation)
    {
        Database db = Database.cons(getApplicationContext());
        BatteryManager batteryManager = (BatteryManager) getApplicationContext().getSystemService(Context.BATTERY_SERVICE);

        DiasyncRenderer renderer = new DiasyncRenderer(
                new WatchDataProvider(db, batteryManager),
                surfaceHolder,
                currentUserStyleRepository,
                watchState,
                CanvasType.HARDWARE,
                1000L);

        return new WatchFace(WatchFaceType.DIGITAL, renderer);
    }

    @Override
    public void onCreate() {
        ContextCompat.startForegroundService(this, new Intent(this, DataSyncService.class));
        batteryStatusReceiver = new BatteryStatusReceiver();
        batteryStatusReceiver.register(getApplicationContext());
    }


    @Override
    public void onDestroy() {
        batteryStatusReceiver.unregister(getApplicationContext());
    }
}