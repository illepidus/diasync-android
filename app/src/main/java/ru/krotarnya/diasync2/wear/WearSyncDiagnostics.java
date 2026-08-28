package ru.krotarnya.diasync2.wear;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.Instant;

public final class WearSyncDiagnostics {
    public enum State {
        NEVER_SENT,
        SENDING,
        SENT,
        FAILED
    }

    private static final String FILE_NAME = "wear_sync_diagnostics";
    private static final String KEY_STATE = "state";
    private static final String KEY_UPDATED_AT = "updated_at";

    private final SharedPreferences preferences;

    public WearSyncDiagnostics(Context context) {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public void record(State state, Instant at) {
        preferences.edit()
                .putString(KEY_STATE, state.name())
                .putLong(KEY_UPDATED_AT, at.toEpochMilli())
                .apply();
    }

    public State state() {
        try {
            return State.valueOf(preferences.getString(KEY_STATE, State.NEVER_SENT.name()));
        } catch (IllegalArgumentException exception) {
            return State.NEVER_SENT;
        }
    }

    public Instant updatedAt() {
        return Instant.ofEpochMilli(preferences.getLong(KEY_UPDATED_AT, 0L));
    }
}
