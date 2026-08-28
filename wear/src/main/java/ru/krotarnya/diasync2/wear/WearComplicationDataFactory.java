package ru.krotarnya.diasync2.wear;

import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.CountUpTimeReference;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.PhotoImageComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.data.SmallImage;
import androidx.wear.watchface.complications.data.SmallImageType;
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText;
import androidx.wear.watchface.complications.data.TimeDifferenceStyle;
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline;
import androidx.wear.watchface.complications.datasource.TimeInterval;
import androidx.wear.watchface.complications.datasource.TimelineEntry;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class WearComplicationDataFactory {
    static final int BITMAP_WIDTH = 360;
    static final int BITMAP_HEIGHT = 235;

    private final WearComplicationRenderer renderer;

    WearComplicationDataFactory() {
        this(new WearComplicationRenderer());
    }

    WearComplicationDataFactory(WearComplicationRenderer renderer) {
        this.renderer = renderer;
    }

    PhotoImageComplicationData photoData(WearComplicationState state) {
        Bitmap bitmap = renderer.render(state, BITMAP_WIDTH, BITMAP_HEIGHT);
        return new PhotoImageComplicationData.Builder(
                Icon.createWithBitmap(bitmap),
                plainText(state.contentDescription()))
                .build();
    }

    ComplicationData shortTextData(WearComplicationState state) {
        if (!state.hasGlucose()) {
            return shortText(state.message(), state.contentDescription());
        }
        if (state.kind() == WearComplicationState.Kind.STALE) {
            return shortAgeData(state);
        }
        return shortText(" ", state.contentDescription());
    }

    ComplicationDataTimeline shortTextTimeline(WearComplicationState state) {
        ComplicationData defaultData = state.hasGlucose()
                ? shortAgeData(state)
                : shortTextData(state);
        if (state.kind() != WearComplicationState.Kind.FRESH) {
            return new ComplicationDataTimeline(defaultData, List.of());
        }
        Instant staleAt = state.latestTimestamp().plus(WearComplicationStateMapper.STALE_AFTER)
                .plusSeconds(1);
        TimelineEntry fresh = new TimelineEntry(
                new TimeInterval(state.latestTimestamp().minusSeconds(1), staleAt),
                shortText(" ", state.contentDescription()));
        return new ComplicationDataTimeline(defaultData, List.of(fresh));
    }

    ComplicationData longTextData(WearComplicationState state) {
        if (!state.hasGlucose()) {
            return longText(plainText(state.message()), state);
        }
        if (state.kind() == WearComplicationState.Kind.STALE) {
            return longText(ageText(state), state);
        }
        return longText(plainText(state.valueWithTrend()), state);
    }

    ComplicationDataTimeline longTextTimeline(WearComplicationState state) {
        ComplicationData defaultData = state.hasGlucose()
                ? longText(ageText(state), state)
                : longTextData(state);
        if (state.kind() != WearComplicationState.Kind.FRESH) {
            return new ComplicationDataTimeline(defaultData, List.of());
        }
        Instant staleAt = state.latestTimestamp().plus(WearComplicationStateMapper.STALE_AFTER)
                .plusSeconds(1);
        TimelineEntry fresh = new TimelineEntry(
                new TimeInterval(state.latestTimestamp().minusSeconds(1), staleAt),
                longText(plainText(state.valueWithTrend()), state));
        return new ComplicationDataTimeline(defaultData, List.of(fresh));
    }

    private ShortTextComplicationData shortAgeData(WearComplicationState state) {
        return new ShortTextComplicationData.Builder(
                ageText(state), plainText(state.contentDescription())).build();
    }

    private TimeDifferenceComplicationText ageText(WearComplicationState state) {
        return new TimeDifferenceComplicationText.Builder(
                TimeDifferenceStyle.SHORT_SINGLE_UNIT,
                new CountUpTimeReference(state.latestTimestamp().plusSeconds(59)))
                .setDisplayAsNow(false)
                .setMinimumTimeUnit(TimeUnit.MINUTES)
                .build();
    }

    private ShortTextComplicationData shortText(String text, String description) {
        return new ShortTextComplicationData.Builder(plainText(text), plainText(description)).build();
    }

    private LongTextComplicationData longText(
            androidx.wear.watchface.complications.data.ComplicationText text,
            WearComplicationState state
    ) {
        Bitmap bitmap = renderer.render(state, BITMAP_WIDTH, BITMAP_HEIGHT);
        SmallImage graph = new SmallImage.Builder(
                Icon.createWithBitmap(bitmap),
                SmallImageType.PHOTO)
                .build();
        return new LongTextComplicationData.Builder(
                text,
                plainText(state.contentDescription()))
                .setSmallImage(graph)
                .build();
    }

    private PlainComplicationText plainText(String text) {
        return new PlainComplicationText.Builder(text).build();
    }
}
