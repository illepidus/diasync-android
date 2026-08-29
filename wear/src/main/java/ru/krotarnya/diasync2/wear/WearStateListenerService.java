package ru.krotarnya.diasync2.wear;

import android.util.Log;
import android.content.Intent;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

public final class WearStateListenerService extends WearableListenerService {
    private static final String TAG = "WearSync";
    public static final String STATE_PATH = "/diasync/v1/state";
    public static final String PAYLOAD_KEY = "snapshot_json";

    private LastKnownWearStateRepository repository;
    private WearAlertController alertController;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = LastKnownWearStateRepository.create(this);
        alertController = WearAlertController.create(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() != DataEvent.TYPE_CHANGED
                    || !STATE_PATH.equals(event.getDataItem().getUri().getPath())) {
                continue;
            }
            try {
                byte[] payload = DataMapItem.fromDataItem(event.getDataItem())
                        .getDataMap()
                        .getByteArray(PAYLOAD_KEY);
                if (!repository.replaceIfValid(payload)) {
                    Log.w(TAG, "Rejected invalid state snapshot");
                    continue;
                }
                WearSnapshot snapshot = repository.load().orElseThrow();
                alertController.onSnapshot(snapshot);
                DiasyncComplicationDataSourceService.requestUpdate(this);
                sendBroadcast(new Intent(WearDiagnosticActivity.ACTION_STATE_UPDATED)
                        .setPackage(getPackageName()));
                String latest = snapshot.points().isEmpty()
                        ? "NO DATA"
                        : Double.toString(snapshot.points().get(0).displayMgDl(
                                snapshot.display().useCalibration()));
                Log.i(TAG, "Stored state generated at " + snapshot.generatedAt()
                        + ", latest=" + latest);
            } catch (RuntimeException exception) {
                Log.w(TAG, "Rejected unreadable state snapshot");
            }
        }
    }
}
