package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.drawable.Icon;

import androidx.test.core.app.ApplicationProvider;
import androidx.wear.watchface.complications.data.PhotoImageComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WearComplicationDataFactoryTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");
    private final WearComplicationDataFactory factory = new WearComplicationDataFactory();

    @Test
    public void photoDataContainsRenderedBitmap() {
        PhotoImageComplicationData data = factory.photoData(state(WearComplicationState.Kind.FRESH));

        assertEquals(Icon.TYPE_BITMAP, data.getPhotoImage().getType());
    }

    @Test
    public void freshTimelineSwitchesFromValueToMinuteAgeAfterStaleBoundary() {
        Context context = ApplicationProvider.getApplicationContext();
        WearComplicationState state = state(WearComplicationState.Kind.FRESH);

        ComplicationDataTimeline timeline = factory.shortTextTimeline(state);
        ShortTextComplicationData fallback =
                (ShortTextComplicationData) timeline.getDefaultComplicationData();
        ShortTextComplicationData fresh = (ShortTextComplicationData) timeline
                .getTimelineEntries().iterator().next().getComplicationData();

        assertEquals(" ", fresh.getText().getTextAt(
                context.getResources(), NOW).toString());
        assertEquals("2m", fallback.getText().getTextAt(
                context.getResources(), NOW.plusSeconds(120)).toString());
        assertEquals(NOW.minusSeconds(29)
                        .plus(WearComplicationStateMapper.STALE_AFTER)
                        .plusSeconds(1),
                timeline.getTimelineEntries().iterator().next().getValidity().getEnd());
    }

    @Test
    public void ambientTimelineKeepsValueUntilItSwitchesToMinuteAge() {
        Context context = ApplicationProvider.getApplicationContext();
        ComplicationDataTimeline timeline = factory.longTextTimeline(
                state(WearComplicationState.Kind.FRESH));
        androidx.wear.watchface.complications.data.LongTextComplicationData fresh =
                (androidx.wear.watchface.complications.data.LongTextComplicationData) timeline
                        .getTimelineEntries().iterator().next().getComplicationData();
        androidx.wear.watchface.complications.data.LongTextComplicationData fallback =
                (androidx.wear.watchface.complications.data.LongTextComplicationData)
                        timeline.getDefaultComplicationData();

        assertEquals("110 →", fresh.getText().getTextAt(
                context.getResources(), NOW).toString());
        assertNotNull(fresh.getSmallImage());
        assertEquals(Icon.TYPE_BITMAP, fresh.getSmallImage().getImage().getType());
        assertEquals("2m", fallback.getText().getTextAt(
                context.getResources(), NOW.plusSeconds(120)).toString());
        assertNotNull(fallback.getSmallImage());
    }

    @Test
    public void noDataTimelineKeepsVisibleStaticFallback() {
        Context context = ApplicationProvider.getApplicationContext();
        ComplicationDataTimeline timeline = factory.shortTextTimeline(
                state(WearComplicationState.Kind.NO_DATA));
        ShortTextComplicationData data =
                (ShortTextComplicationData) timeline.getDefaultComplicationData();

        assertTrue(timeline.getTimelineEntries().isEmpty());
        assertEquals("NO DATA", data.getText().getTextAt(
                context.getResources(), NOW).toString());
    }

    private WearComplicationState state(WearComplicationState.Kind kind) {
        if (kind == WearComplicationState.Kind.NO_DATA) {
            return new WearComplicationState(
                    kind, NOW, null, List.of(), Double.NaN, "", "", "",
                    70.0, 180.0, 30, false, false, "NO DATA", "No glucose data");
        }
        return new WearComplicationState(
                kind,
                NOW,
                NOW.minusSeconds(29),
                List.of(new WearComplicationState.GraphPoint(NOW.minusSeconds(29), 110.0)),
                110.0,
                "110",
                "→",
                "mg/dL",
                70.0,
                180.0,
                30,
                true,
                true,
                "",
                "Glucose 110 mg/dL, trend →");
    }
}
