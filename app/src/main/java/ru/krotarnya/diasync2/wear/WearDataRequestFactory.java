package ru.krotarnya.diasync2.wear;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import java.util.Objects;

public final class WearDataRequestFactory {
    public static final String STATE_PATH = "/diasync/v1/state";
    public static final String PAYLOAD_KEY = "snapshot_json";

    public PutDataRequest create(byte[] payload) {
        Objects.requireNonNull(payload);
        PutDataMapRequest mapRequest = PutDataMapRequest.create(STATE_PATH);
        mapRequest.getDataMap().putByteArray(PAYLOAD_KEY, payload);
        return mapRequest.asPutDataRequest().setUrgent();
    }
}
