package ru.krotarnya.diasync2.widget;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAppWidgetManager;
import ru.krotarnya.diasync2.MainActivity;
import ru.krotarnya.diasync2.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WidgetRoutingTest {
    @Test
    public void explicitUpdateIncludesEveryExistingWidgetId() {
        Application application = RuntimeEnvironment.getApplication();
        AppWidgetManager manager = AppWidgetManager.getInstance(application);
        ShadowAppWidgetManager shadowManager = shadowOf(manager);
        int first = shadowManager.createWidget(
                DiasyncWidgetProvider.class,
                R.layout.widget_latest_value);
        int second = shadowManager.createWidget(
                DiasyncWidgetProvider.class,
                R.layout.widget_latest_value);

        DiasyncWidgetProvider.requestUpdate(application);

        List<Intent> broadcasts = shadowOf(application).getBroadcastIntents();
        Intent update = broadcasts.get(broadcasts.size() - 1);
        assertEquals(AppWidgetManager.ACTION_APPWIDGET_UPDATE, update.getAction());
        assertEquals(
                new ComponentName(application, DiasyncWidgetProvider.class),
                update.getComponent());
        assertArrayEquals(
                new int[]{first, second},
                update.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
    }

    @Test
    public void updateIsSkippedWhenNoWidgetExists() {
        Application application = RuntimeEnvironment.getApplication();
        int before = shadowOf(application).getBroadcastIntents().size();

        DiasyncWidgetProvider.requestUpdate(application);

        assertEquals(before, shadowOf(application).getBroadcastIntents().size());
    }

    @Test
    public void rootClickOpensStatusActivity() {
        Application application = RuntimeEnvironment.getApplication();
        WidgetState state = new WidgetState(
                "5.6",
                "mmol/L",
                "→",
                "",
                WidgetState.Range.NORMAL,
                true,
                false);
        RemoteViews remoteViews = new WidgetRemoteViewsFactory().create(application, state);
        View view = remoteViews.apply(application, new FrameLayout(application));

        assertNotNull(view.findViewById(R.id.widget_root));
        assertTrue(view.findViewById(R.id.widget_root).performClick());

        Intent started = shadowOf(application).getNextStartedActivity();
        assertEquals(
                new ComponentName(application, MainActivity.class),
                started.getComponent());
    }
}
