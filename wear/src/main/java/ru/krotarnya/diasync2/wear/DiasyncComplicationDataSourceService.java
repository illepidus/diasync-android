package ru.krotarnya.diasync2.wear;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

public final class DiasyncComplicationDataSourceService extends ComplicationDataSourceService {
    private static final String TAG = "Complication";
    private static final Instant PREVIEW_TIME = Instant.parse("2026-08-28T12:00:00Z");

    private LastKnownWearStateRepository repository;
    private WearComplicationStateMapper mapper;
    private WearComplicationDataFactory dataFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = LastKnownWearStateRepository.create(this);
        mapper = new WearComplicationStateMapper();
        dataFactory = new WearComplicationDataFactory();
    }

    @Override
    public void onComplicationRequest(
            ComplicationRequest request,
            @NonNull ComplicationRequestListener listener) {
        WearComplicationState state = mapper.map(repository.load());
        if (request.getComplicationType() == ComplicationType.PHOTO_IMAGE) {
            reply(listener, dataFactory.photoData(state));
        } else if (request.getComplicationType() == ComplicationType.SHORT_TEXT) {
            replyTimeline(listener, dataFactory.shortTextTimeline(state));
        } else if (request.getComplicationType() == ComplicationType.LONG_TEXT) {
            replyTimeline(listener, dataFactory.longTextTimeline(state));
        } else {
            reply(listener, null);
        }
    }

    @Override
    public ComplicationData getPreviewData(@NonNull ComplicationType type) {
        WearComplicationState preview = previewState();
        if (type == ComplicationType.PHOTO_IMAGE) {
            return dataFactory.photoData(preview);
        }
        if (type == ComplicationType.SHORT_TEXT) {
            return dataFactory.shortTextData(preview);
        }
        return type == ComplicationType.LONG_TEXT ? dataFactory.longTextData(preview) : null;
    }

    static void requestUpdate(Context context) {
        ComplicationDataSourceUpdateRequester.create(
                        context,
                        new ComponentName(context, DiasyncComplicationDataSourceService.class))
                .requestUpdateAll();
    }

    private WearComplicationState previewState() {
        WearSnapshot snapshot = new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                PREVIEW_TIME,
                List.of(
                        new WearGlucosePoint(PREVIEW_TIME.minusSeconds(30), 121.0, null, null),
                        new WearGlucosePoint(PREVIEW_TIME.minusSeconds(330), 113.0, null, null),
                        new WearGlucosePoint(PREVIEW_TIME.minusSeconds(630), 106.0, null, null)),
                new WearDisplayPolicy(
                        GlucoseUnit.MMOL_L,
                        true,
                        70.0,
                        180.0,
                        30,
                        true,
                        true,
                        true,
                        "↗"),
                new WearAlertPolicy(false, false, false, Instant.EPOCH),
                null);
        return new WearComplicationStateMapper(
                Clock.fixed(PREVIEW_TIME, ZoneOffset.UTC)).map(Optional.of(snapshot));
    }

    private void reply(ComplicationRequestListener listener, ComplicationData data) {
        try {
            listener.onComplicationData(data);
        } catch (RemoteException exception) {
            Log.w(TAG, "Complication request client disconnected");
        }
    }

    private void replyTimeline(
            ComplicationRequestListener listener,
            androidx.wear.watchface.complications.datasource.ComplicationDataTimeline timeline
    ) {
        try {
            listener.onComplicationDataTimeline(timeline);
        } catch (RemoteException exception) {
            Log.w(TAG, "Complication request client disconnected");
        }
    }
}
