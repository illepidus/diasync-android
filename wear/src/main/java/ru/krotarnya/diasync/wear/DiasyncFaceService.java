package ru.krotarnya.diasync.wear;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.wear.watchface.CanvasType;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.WatchFace;
import androidx.wear.watchface.WatchFaceService;
import androidx.wear.watchface.WatchFaceType;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import kotlin.coroutines.Continuation;
import ru.krotarnya.diasync.common.receiver.NewDataReceiver;
import ru.krotarnya.diasync.common.repository.DiasyncDatabase;
import ru.krotarnya.diasync.common.service.DataSyncService;

@SuppressLint("Deprecated")
public class DiasyncFaceService extends WatchFaceService {
    @Override
    public WatchFace createWatchFace(
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository,
            @NonNull Continuation<? super WatchFace> continuation) {
        DiasyncCanvasRenderer renderer = new DiasyncCanvasRenderer(
                surfaceHolder,
                currentUserStyleRepository,
                watchState,
                CanvasType.HARDWARE,
                1000L);

        DiasyncDatabase db = DiasyncDatabase.cons(getApplicationContext());
        NewDataReceiver receiver = new NewDataReceiver(db, renderer::update);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, receiver.intentFilter());

        return new WatchFace(WatchFaceType.DIGITAL, renderer);
    }

    @Override
    public void onCreate() {
        ContextCompat.startForegroundService(this, new Intent(this, DataSyncService.class));
    }
}