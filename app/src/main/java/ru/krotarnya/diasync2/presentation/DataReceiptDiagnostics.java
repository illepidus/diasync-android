package ru.krotarnya.diasync2.presentation;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.Instant;

public final class DataReceiptDiagnostics {
    private static final String FILE = "data_receipt_diagnostics";
    private static final String KEY_RECEIVED_AT = "received_at";
    private final SharedPreferences preferences;

    public DataReceiptDiagnostics(Context context) {
        preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public void record(Instant at) {
        preferences.edit().putLong(KEY_RECEIVED_AT, at.toEpochMilli()).apply();
    }

    public Instant receivedAt() {
        long value = preferences.getLong(KEY_RECEIVED_AT, Long.MIN_VALUE);
        return value == Long.MIN_VALUE ? null : Instant.ofEpochMilli(value);
    }
}
