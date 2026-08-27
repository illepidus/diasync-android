package ru.krotarnya.diasync2.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.widget.DiasyncWidgetProvider;

public final class PhoneUpdateCoordinator {
    public interface Listener {
        void onSyncStateChanged(SyncConnectionState state, boolean dataChanged);
    }

    private final Context context;
    private final AppPreferences preferences;
    private final Handler mainHandler;
    private final AtomicReference<Listener> listener = new AtomicReference<>();

    public PhoneUpdateCoordinator(Context context, AppPreferences preferences) {
        this.context = context.getApplicationContext();
        this.preferences = Objects.requireNonNull(preferences);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void register(Listener listener) {
        this.listener.set(Objects.requireNonNull(listener));
    }

    public void unregister(Listener listener) {
        this.listener.compareAndSet(listener, null);
    }

    public void stateChanged(SyncConnectionState state) {
        preferences.saveSyncConnectionState(state);
        notifyListener(state, false);
    }

    public void dataCommitted() {
        DiasyncWidgetProvider.requestUpdate(context);
        notifyListener(preferences.syncConnectionState(), true);
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
