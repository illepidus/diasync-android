package ru.krotarnya.diasync2.wear;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.Instant;

final class SharedPreferencesWearAlertStateStore implements WearAlertStateStore {
    private static final String FILE_NAME = "wear_alert_state";
    private static final String KEY_EVENT_ID = "last_processed_event_id";
    private static final String KEY_NO_DATA_AT = "last_no_data_alert_at";
    private static final String KEY_DATA_PHASE = "data_phase";

    private final SharedPreferences preferences;

    SharedPreferencesWearAlertStateStore(Context context) {
        preferences = context.createDeviceProtectedStorageContext()
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public WearAlertState read() {
        String eventId = preferences.getString(KEY_EVENT_ID, null);
        long noDataAtMillis = preferences.getLong(KEY_NO_DATA_AT, Long.MIN_VALUE);
        WearDataPhase phase;
        try {
            phase = WearDataPhase.valueOf(preferences.getString(
                    KEY_DATA_PHASE,
                    WearDataPhase.FRESH.name()));
        } catch (IllegalArgumentException exception) {
            phase = WearDataPhase.FRESH;
        }
        return new WearAlertState(
                eventId,
                noDataAtMillis == Long.MIN_VALUE ? null : Instant.ofEpochMilli(noDataAtMillis),
                phase);
    }

    @Override
    public boolean write(WearAlertState state) {
        SharedPreferences.Editor editor = preferences.edit()
                .putString(KEY_EVENT_ID, state.lastProcessedEventId())
                .putString(KEY_DATA_PHASE, state.dataPhase().name());
        if (state.lastNoDataAlertAt() == null) {
            editor.remove(KEY_NO_DATA_AT);
        } else {
            editor.putLong(KEY_NO_DATA_AT, state.lastNoDataAlertAt().toEpochMilli());
        }
        return editor.commit();
    }
}
