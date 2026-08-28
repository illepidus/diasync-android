package ru.krotarnya.diasync2.wear;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;

public final class DiasyncComplicationDataSourceService extends ComplicationDataSourceService {
    private static final String TAG = "Complication";
    private static final WearComplicationState PREVIEW_STATE =
            new WearComplicationState("6.7 →", "mmol/L", "Glucose 6.7 mmol/L, trend right");

    private LastKnownWearStateRepository repository;
    private WearComplicationStateMapper mapper;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = LastKnownWearStateRepository.create(this);
        mapper = new WearComplicationStateMapper();
    }

    @Override
    public void onComplicationRequest(
            ComplicationRequest request,
            ComplicationRequestListener listener) {
        if (request.getComplicationType() != ComplicationType.SHORT_TEXT) {
            reply(listener, null);
            return;
        }
        reply(listener, toData(mapper.map(repository.load())));
    }

    @Override
    public ComplicationData getPreviewData(ComplicationType type) {
        return type == ComplicationType.SHORT_TEXT ? toData(PREVIEW_STATE) : null;
    }

    static void requestUpdate(Context context) {
        ComplicationDataSourceUpdateRequester.create(
                        context,
                        new ComponentName(context, DiasyncComplicationDataSourceService.class))
                .requestUpdateAll();
    }

    private ShortTextComplicationData toData(WearComplicationState state) {
        return new ShortTextComplicationData.Builder(
                        plainText(state.text()), plainText(state.contentDescription()))
                .setTitle(plainText(state.title()))
                .build();
    }

    private PlainComplicationText plainText(String text) {
        return new PlainComplicationText.Builder(text).build();
    }

    private void reply(ComplicationRequestListener listener, ComplicationData data) {
        try {
            listener.onComplicationData(data);
        } catch (RemoteException exception) {
            Log.w(TAG, "Complication request client disconnected");
        }
    }
}
