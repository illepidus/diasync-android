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
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAppWidgetManager;
import ru.krotarnya.diasync2.DiasyncApplication;
import ru.krotarnya.diasync2.R;
import ru.krotarnya.diasync2.settings.GraphWindow;

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
    public void configurationChangeRequestsEveryWidgetUpdate() {
        Application application = RuntimeEnvironment.getApplication();
        AppWidgetManager manager = AppWidgetManager.getInstance(application);
        ShadowAppWidgetManager shadowManager = shadowOf(manager);
        int widgetId = shadowManager.createWidget(
                DiasyncWidgetProvider.class,
                R.layout.widget_latest_value);

        Configuration landscape = new Configuration(application.getResources().getConfiguration());
        landscape.orientation = Configuration.ORIENTATION_LANDSCAPE;
        ((DiasyncApplication) application).onConfigurationChanged(landscape);

        List<Intent> broadcasts = shadowOf(application).getBroadcastIntents();
        Intent update = broadcasts.get(broadcasts.size() - 1);
        assertEquals(AppWidgetManager.ACTION_APPWIDGET_UPDATE, update.getAction());
        assertArrayEquals(
                new int[]{widgetId},
                update.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
    }

    @Test
    public void rootClickDispatchesTapToWidgetProvider() {
        Application application = RuntimeEnvironment.getApplication();
        WidgetState state = new WidgetState(
                "5.6",
                "→",
                "",
                WidgetState.Range.NORMAL,
                true,
                false,
                Instant.parse("2026-08-27T12:00:00Z"),
                List.of(),
                GraphWindow.THIRTY_MINUTES,
                70.0,
                180.0,
                true,
                false,
                true,
                false);
        RemoteViews remoteViews = new WidgetRemoteViewsFactory().create(
                application,
                state,
                null,
                widgetSize(180, 110));
        View view = remoteViews.apply(application, new FrameLayout(application));

        assertNotNull(view.findViewById(R.id.widget_root));
        assertTrue(view.findViewById(R.id.widget_root).performClick());

        List<Intent> broadcasts = shadowOf(application).getBroadcastIntents();
        Intent started = broadcasts.get(broadcasts.size() - 1);
        assertEquals(
                new ComponentName(application, DiasyncWidgetProvider.class),
                started.getComponent());
        assertEquals(DiasyncWidgetProvider.ACTION_WIDGET_TAP, started.getAction());
    }

    @Test
    public void presentationTextGrowsWithWidgetArea() {
        Application application = RuntimeEnvironment.getApplication();
        WidgetState state = new WidgetState(
                "5.6",
                "→",
                "",
                WidgetState.Range.NORMAL,
                true,
                false,
                Instant.parse("2026-08-27T12:00:00Z"),
                List.of(),
                GraphWindow.THIRTY_MINUTES,
                70.0,
                180.0,
                true,
                false,
                true,
                false);

        RemoteViews compactViews = new WidgetRemoteViewsFactory().create(
                application,
                state,
                null,
                widgetSize(70, 70));
        RemoteViews largeViews = new WidgetRemoteViewsFactory().create(
                application,
                state,
                null,
                widgetSize(220, 440));
        View compactView = compactViews.apply(application, new FrameLayout(application));
        View largeView = largeViews.apply(application, new FrameLayout(application));

        TextView compactValue = compactView.findViewById(R.id.widget_value);
        TextView largeValue = largeView.findViewById(R.id.widget_value);
        assertEquals(22.0f, compactValue.getTextSize(), 0.01f);
        assertTrue(largeValue.getTextSize() > compactValue.getTextSize());
        ImageView trend = largeView.findViewById(R.id.widget_trend);
        assertNotNull(trend.getDrawable());
    }

    @Test
    public void valueUsesCanonicalRangeColors() {
        Application application = RuntimeEnvironment.getApplication();

        assertValueColor(application, WidgetState.Range.NORMAL, R.color.widget_normal);
        assertValueColor(application, WidgetState.Range.HIGH, R.color.widget_high);
        assertValueColor(application, WidgetState.Range.LOW, R.color.widget_low);
    }

    @Test
    public void trendUsesTheSameRangeColorAsValue() {
        WidgetRemoteViewsFactory factory = new WidgetRemoteViewsFactory();

        assertEquals(R.color.widget_normal, factory.colorResource(WidgetState.Range.NORMAL));
        assertEquals(R.color.widget_high, factory.colorResource(WidgetState.Range.HIGH));
        assertEquals(R.color.widget_low, factory.colorResource(WidgetState.Range.LOW));
    }

    @Test
    public void ageMessageIsBoldAndCenteredNearTheBottomEdge() {
        Application application = RuntimeEnvironment.getApplication();
        WidgetState state = new WidgetState(
                "5.6",
                "→",
                "2m ago",
                WidgetState.Range.NORMAL,
                true,
                false,
                Instant.parse("2026-08-27T12:00:00Z"),
                List.of(),
                GraphWindow.THIRTY_MINUTES,
                70.0,
                180.0,
                true,
                false,
                true,
                false);
        View view = new WidgetRemoteViewsFactory().create(
                application,
                state,
                null,
                widgetSize(180, 110)).apply(application, new FrameLayout(application));
        TextView message = view.findViewById(R.id.widget_message);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) message.getLayoutParams();
        int expectedBottomMargin = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8.0f,
                application.getResources().getDisplayMetrics()));

        assertEquals(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, params.gravity);
        assertEquals(expectedBottomMargin, params.bottomMargin);
        assertEquals(
                Gravity.CENTER_HORIZONTAL,
                message.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK);
        assertEquals(Typeface.BOLD, message.getTypeface().getStyle());
    }

    @Test
    public void trendArrowCanBeHiddenWithoutLeavingLayoutSpace() {
        Application application = RuntimeEnvironment.getApplication();
        WidgetState state = new WidgetState(
                "5.6",
                "→",
                "",
                WidgetState.Range.NORMAL,
                true,
                false,
                Instant.parse("2026-08-27T12:00:00Z"),
                List.of(),
                GraphWindow.THIRTY_MINUTES,
                70.0,
                180.0,
                true,
                false,
                false,
                false);

        RemoteViews remoteViews = new WidgetRemoteViewsFactory().create(
                application,
                state,
                null,
                widgetSize(180, 110));
        View view = remoteViews.apply(application, new FrameLayout(application));

        assertEquals(View.GONE, view.findViewById(R.id.widget_trend).getVisibility());
    }

    @Test
    public void graphPreservesItsAspectRatioWithinTheEntireWidgetBounds() {
        Application application = RuntimeEnvironment.getApplication();
        WidgetState state = new WidgetState(
                "5.6",
                "→",
                "",
                WidgetState.Range.NORMAL,
                true,
                false,
                Instant.parse("2026-08-27T12:00:00Z"),
                List.of(),
                GraphWindow.THIRTY_MINUTES,
                70.0,
                180.0,
                true,
                false,
                true,
                true);

        View view = new WidgetRemoteViewsFactory().create(
                application,
                state,
                null,
                widgetSize(600, 100)).apply(application, new FrameLayout(application));
        ImageView graph = view.findViewById(R.id.widget_graph);

        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, graph.getLayoutParams().width);
        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, graph.getLayoutParams().height);
        assertEquals(ImageView.ScaleType.FIT_CENTER, graph.getScaleType());
    }

    private void assertValueColor(Application application, WidgetState.Range range, int colorId) {
        WidgetState state = new WidgetState(
                "100",
                "→",
                "",
                range,
                true,
                false,
                Instant.parse("2026-08-27T12:00:00Z"),
                List.of(),
                GraphWindow.THIRTY_MINUTES,
                70.0,
                180.0,
                true,
                false,
                true,
                false);
        RemoteViews remoteViews = new WidgetRemoteViewsFactory().create(
                application,
                state,
                null,
                widgetSize(180, 110));
        View view = remoteViews.apply(application, new FrameLayout(application));
        TextView value = view.findViewById(R.id.widget_value);

        assertEquals(application.getColor(colorId), value.getCurrentTextColor());
    }

    private WidgetBitmapSize widgetSize(int widthDp, int heightDp) {
        return new WidgetBitmapSize(widthDp, heightDp, widthDp, heightDp);
    }
}
