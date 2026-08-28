package ru.krotarnya.diasync2.wear;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.util.Objects;

final class SharedPreferencesWearSnapshotStore implements WearSnapshotStore {
    private static final String FILE_NAME = "wear_last_known_state";
    private static final String KEY_PAYLOAD = "snapshot_payload";

    private final SharedPreferences preferences;

    SharedPreferencesWearSnapshotStore(Context context) {
        Context deviceContext = context.createDeviceProtectedStorageContext();
        preferences = deviceContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public byte[] read() {
        String encoded = preferences.getString(KEY_PAYLOAD, null);
        if (encoded == null) {
            return null;
        }
        try {
            return Base64.decode(encoded, Base64.NO_WRAP);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public boolean write(byte[] payload) {
        Objects.requireNonNull(payload);
        return preferences.edit()
                .putString(KEY_PAYLOAD, Base64.encodeToString(payload, Base64.NO_WRAP))
                .commit();
    }
}
