package ru.krotarnya.diasync2.presentation;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class DiagnosticEventLog {
    private static final String FILE = "diagnostic_events";
    private static final String KEY_EVENTS = "events";
    private static final String SEPARATOR = "\u001e";
    private static final int LIMIT = 50;
    private final SharedPreferences preferences;

    public DiagnosticEventLog(Context context) {
        preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public synchronized void record(String category, String message, Instant at) {
        List<String> events = new ArrayList<>(latest());
        events.add(at + " · " + safe(category) + " · " + safe(message));
        if (events.size() > LIMIT) {
            events = new ArrayList<>(events.subList(events.size() - LIMIT, events.size()));
        }
        preferences.edit().putString(KEY_EVENTS, String.join(SEPARATOR, events)).apply();
    }

    public synchronized List<String> latest() {
        String stored = preferences.getString(KEY_EVENTS, "");
        return stored.isEmpty() ? List.of() : List.of(stored.split(SEPARATOR, -1));
    }

    private String safe(String value) {
        return value.replace('\n', ' ').replace('\r', ' ').replace(SEPARATOR, " ");
    }
}
