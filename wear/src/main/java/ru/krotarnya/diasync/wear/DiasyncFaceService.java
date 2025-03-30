package ru.krotarnya.diasync.wear;

import android.annotation.SuppressLint;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.CanvasType;
import androidx.wear.watchface.WatchFace;
import androidx.wear.watchface.WatchFaceService;
import androidx.wear.watchface.WatchFaceType;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.style.CurrentUserStyleRepository;
import kotlin.coroutines.Continuation;

@SuppressLint("Deprecated")
public class DiasyncFaceService extends WatchFaceService {
    @NonNull
    @Override
    public Object createWatchFace(
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
                1000L
        );
        return new WatchFace(WatchFaceType.DIGITAL, renderer);
    }

}