package ru.krotarnya.diasync2.wear;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.Instant;

final class WearReceiveDiagnostics {
    private static final String FILE = "wear_receive_diagnostics";
    private final SharedPreferences preferences;

    WearReceiveDiagnostics(Context context) {
        preferences = context.createDeviceProtectedStorageContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    void accepted(Instant now) {
        preferences.edit().putLong("received_at", now.toEpochMilli()).remove("last_error").apply();
    }

    void rejected(String safeError) {
        preferences.edit().putString("last_error", safeError).apply();
    }

    Instant receivedAt() {
        long value = preferences.getLong("received_at", Long.MIN_VALUE);
        return value == Long.MIN_VALUE ? null : Instant.ofEpochMilli(value);
    }

    String lastError() { return preferences.getString("last_error", null); }
}
