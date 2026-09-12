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
        if (stored.isEmpty()) {
            return List.of();
        }
        List<String> events = new ArrayList<>();
        for (String event : stored.split(SEPARATOR, -1)) {
            if (!isLegacyMasterLoopState(event)) {
                events.add(event);
            }
        }
        return List.copyOf(events);
    }

    private boolean isLegacyMasterLoopState(String event) {
        return event.endsWith(" · Sync · WAITING_FOR_XDRIP")
                || event.endsWith(" · Sync · UPLOADING");
    }

    private String safe(String value) {
        return value.replace('\n', ' ').replace('\r', ' ').replace(SEPARATOR, " ");
    }
}
