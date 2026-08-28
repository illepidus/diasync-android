package ru.krotarnya.diasync2.sync;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Looper;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAppWidgetManager;
import ru.krotarnya.diasync2.R;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.widget.DiasyncWidgetProvider;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class PhoneUpdateCoordinatorTest {
    @Test
    public void committedDataNotifiesActivityAndEveryExistingWidget() {
        Application application = RuntimeEnvironment.getApplication();
        AppWidgetManager manager = AppWidgetManager.getInstance(application);
        ShadowAppWidgetManager shadowManager = shadowOf(manager);
        int widgetId = shadowManager.createWidget(
                DiasyncWidgetProvider.class,
                R.layout.widget_latest_value);
        AppPreferences preferences = new AppPreferences(application);
        preferences.saveSyncConnectionState(SyncConnectionState.CONNECTED);
        PhoneUpdateCoordinator coordinator = new PhoneUpdateCoordinator(application, preferences);
        RecordingListener listener = new RecordingListener();
        coordinator.register(listener);

        coordinator.dataCommitted();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(SyncConnectionState.CONNECTED, listener.state);
        assertTrue(listener.dataChanged);
        List<Intent> broadcasts = shadowOf(application).getBroadcastIntents();
        Intent update = broadcasts.get(broadcasts.size() - 1);
        assertEquals(AppWidgetManager.ACTION_APPWIDGET_UPDATE, update.getAction());
        assertEquals(
                new ComponentName(application, DiasyncWidgetProvider.class),
                update.getComponent());
        assertArrayEquals(
                new int[]{widgetId},
                update.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
    }

    @Test
    public void committedDataRequestsAlertCheck() {
        Application application = RuntimeEnvironment.getApplication();
        AtomicBoolean checked = new AtomicBoolean();
        AtomicBoolean wearUpdated = new AtomicBoolean();
        PhoneUpdateCoordinator coordinator = new PhoneUpdateCoordinator(
                application,
                new AppPreferences(application),
                () -> checked.set(true),
                () -> wearUpdated.set(true));

        coordinator.dataCommitted();

        assertTrue(checked.get());
        assertTrue(wearUpdated.get());
    }

    private static final class RecordingListener implements PhoneUpdateCoordinator.Listener {
        private SyncConnectionState state;
        private boolean dataChanged;

        @Override
        public void onSyncStateChanged(SyncConnectionState state, boolean dataChanged) {
            this.state = state;
            this.dataChanged = dataChanged;
        }
    }
}
