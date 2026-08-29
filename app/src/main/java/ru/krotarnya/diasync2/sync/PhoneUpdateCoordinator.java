package ru.krotarnya.diasync2.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Clock;
import ru.krotarnya.diasync2.presentation.DiagnosticEventLog;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.widget.DiasyncWidgetProvider;

public final class PhoneUpdateCoordinator {
    public interface Listener {
        void onSyncStateChanged(SyncConnectionState state, boolean dataChanged);
    }

    private final Context context;
    private final AppPreferences preferences;
    private final Handler mainHandler;
    private final Runnable alertCheck;
    private final Runnable wearUpdate;
    private final AtomicReference<Listener> listener = new AtomicReference<>();
    private final DiagnosticEventLog eventLog;
    private final Clock clock;

    public PhoneUpdateCoordinator(Context context, AppPreferences preferences) {
        this(context, preferences, () -> { }, () -> { });
    }

    public PhoneUpdateCoordinator(
            Context context,
            AppPreferences preferences,
            Runnable alertCheck
    ) {
        this(context, preferences, alertCheck, () -> { });
    }

    public PhoneUpdateCoordinator(
            Context context,
            AppPreferences preferences,
            Runnable alertCheck,
            Runnable wearUpdate
    ) {
        this(context, preferences, alertCheck, wearUpdate, null, Clock.systemUTC());
    }

    public PhoneUpdateCoordinator(
            Context context,
            AppPreferences preferences,
            Runnable alertCheck,
            Runnable wearUpdate,
            DiagnosticEventLog eventLog,
            Clock clock
    ) {
        this.context = context.getApplicationContext();
        this.preferences = Objects.requireNonNull(preferences);
        this.alertCheck = Objects.requireNonNull(alertCheck);
        this.wearUpdate = Objects.requireNonNull(wearUpdate);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.eventLog = eventLog;
        this.clock = Objects.requireNonNull(clock);
    }

    public void register(Listener listener) {
        this.listener.set(Objects.requireNonNull(listener));
    }

    public void unregister(Listener listener) {
        this.listener.compareAndSet(listener, null);
    }

    public void stateChanged(SyncConnectionState state) {
        preferences.saveSyncConnectionState(state);
        record("Sync", state.name());
        notifyListener(state, false);
    }

    public void dataCommitted() {
        record("Sync", "Data batch committed");
        alertCheck.run();
        wearUpdate.run();
        DiasyncWidgetProvider.requestUpdate(context);
        notifyListener(preferences.syncConnectionState(), true);
    }

    private void record(String category, String message) {
        if (eventLog != null) {
            eventLog.record(category, message, clock.instant());
        }
    }

    private void notifyListener(SyncConnectionState state, boolean dataChanged) {
        mainHandler.post(() -> {
            Listener current = listener.get();
            if (current != null) {
                current.onSyncStateChanged(state, dataChanged);
            }
        });
    }
}
