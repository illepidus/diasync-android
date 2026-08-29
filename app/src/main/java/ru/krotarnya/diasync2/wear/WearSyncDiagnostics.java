package ru.krotarnya.diasync2.wear;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.Instant;
import ru.krotarnya.diasync2.presentation.DiagnosticEventLog;

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
    private final DiagnosticEventLog eventLog;

    public WearSyncDiagnostics(Context context) {
        this(context, null);
    }

    public WearSyncDiagnostics(Context context, DiagnosticEventLog eventLog) {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        this.eventLog = eventLog;
    }

    public void record(State state, Instant at) {
        preferences.edit()
                .putString(KEY_STATE, state.name())
                .putLong(KEY_UPDATED_AT, at.toEpochMilli())
                .apply();
        if (eventLog != null) {
            eventLog.record("Wear", "Snapshot " + state.name(), at);
        }
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
