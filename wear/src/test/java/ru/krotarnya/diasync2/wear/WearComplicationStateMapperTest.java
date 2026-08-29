package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

public class WearComplicationStateMapperTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");
    private final WearComplicationStateMapper mapper = new WearComplicationStateMapper(
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    public void missingAndEmptySnapshotsMapToVisibleNoData() {
        assertEquals(WearComplicationState.Kind.NO_DATA, mapper.map(Optional.empty()).kind());
        assertEquals("NO DATA", mapper.map(Optional.empty()).message());
        assertEquals(WearComplicationState.Kind.NO_DATA,
                mapper.map(Optional.of(snapshot(30, "", List.of()))).kind());
    }

    @Test
    public void normalDatasetMapsCalibratedValueTrendAndConfiguredGraph() {
        WearComplicationState state = mapper.map(Optional.of(snapshot(
                30,
                "→",
                List.of(
                        new WearGlucosePoint(NOW.minusSeconds(30), 108.0, 1.1, 1.2),
                        new WearGlucosePoint(NOW.minusSeconds(330), 105.0, null, null)))));

        assertEquals(WearComplicationState.Kind.FRESH, state.kind());
        assertEquals("6.7 →", state.valueWithTrend());
        assertEquals("mmol/L", state.unit());
        assertEquals(2, state.points().size());
    }

    @Test
    public void lowFallingAndHighRisingDatasetsKeepRangeAndTrend() {
        WearComplicationState low = mapper.map(Optional.of(snapshot(
                30,
                "↓",
                List.of(new WearGlucosePoint(NOW.minusSeconds(20), 62.0, null, null)))));
        WearComplicationState high = mapper.map(Optional.of(snapshot(
                30,
                "↑",
                List.of(new WearGlucosePoint(NOW.minusSeconds(20), 205.0, null, null)))));

        assertEquals(WearComplicationRenderer.LOW_COLOR, WearComplicationRenderer.rangeColor(
                low.latestDisplayMgDl(), low.lowMgDl(), low.highMgDl()));
        assertEquals("3.4 ↓", low.valueWithTrend());
        assertEquals(WearComplicationRenderer.HIGH_COLOR, WearComplicationRenderer.rangeColor(
                high.latestDisplayMgDl(), high.lowMgDl(), high.highMgDl()));
        assertEquals("11.4 ↑", high.valueWithTrend());
    }

    @Test
    public void staleStartsAfterNinetySecondsAndShowsMinuteAge() {
        WearComplicationState atBoundary = mapper.map(Optional.of(snapshot(
                30,
                "→",
                List.of(new WearGlucosePoint(NOW.minusSeconds(90), 110.0, null, null)))));
        WearComplicationState stale = mapper.map(Optional.of(snapshot(
                30,
                "→",
                List.of(new WearGlucosePoint(NOW.minusSeconds(121), 110.0, null, null)))));

        assertEquals(WearComplicationState.Kind.FRESH, atBoundary.kind());
        assertEquals(WearComplicationState.Kind.STALE, stale.kind());
        assertEquals("2m", stale.message());
    }

    @Test
    public void noDataPresentationStartsAtFiveMinutes() {
        WearComplicationState before = mapper.map(Optional.of(snapshot(
                30,
                "→",
                List.of(new WearGlucosePoint(NOW.minusSeconds(299), 110.0, null, null)))));
        WearComplicationState atBoundary = mapper.map(Optional.of(snapshot(
                30,
                "→",
                List.of(new WearGlucosePoint(NOW.minusSeconds(300), 110.0, null, null)))));

        assertEquals(WearComplicationState.Kind.STALE, before.kind());
        assertEquals(WearComplicationState.Kind.NO_DATA, atBoundary.kind());
        assertEquals("NO DATA", atBoundary.message());
    }

    @Test
    public void pointBeyondFutureToleranceMapsToVisibleError() {
        WearSnapshot future = snapshot(
                NOW.plusSeconds(1),
                30,
                "",
                List.of(new WearGlucosePoint(NOW.plusSeconds(61), 110.0, null, null)));

        WearComplicationState state = mapper.map(Optional.of(future));

        assertEquals(WearComplicationState.Kind.FUTURE, state.kind());
        assertEquals("FUTURE DATA", state.message());
    }

    @Test
    public void graphWindowsBoundPointsAtThirtySixtyAndOneHundredEightyMinutes() {
        List<WearGlucosePoint> points = List.of(
                new WearGlucosePoint(NOW.minusSeconds(30), 110.0, null, null),
                new WearGlucosePoint(NOW.minusSeconds(45 * 60), 115.0, null, null),
                new WearGlucosePoint(NOW.minusSeconds(120 * 60), 120.0, null, null));

        assertEquals(1, mapper.map(Optional.of(snapshot(30, "", points))).points().size());
        assertEquals(2, mapper.map(Optional.of(snapshot(60, "", points))).points().size());
        assertEquals(3, mapper.map(Optional.of(snapshot(180, "", points))).points().size());
        assertTrue(mapper.map(Optional.of(snapshot(180, "", points))).graphLines());
    }

    private WearSnapshot snapshot(
            int graphWindowMinutes,
            String trend,
            List<WearGlucosePoint> points
    ) {
        return snapshot(NOW, graphWindowMinutes, trend, points);
    }

    private WearSnapshot snapshot(
            Instant generatedAt,
            int graphWindowMinutes,
            String trend,
            List<WearGlucosePoint> points
    ) {
        return new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                generatedAt,
                points,
                new WearDisplayPolicy(
                        GlucoseUnit.MMOL_L,
                        true,
                        70.0,
                        180.0,
                        graphWindowMinutes,
                        true,
                        true,
                        true,
                        trend),
                new WearAlertPolicy(false, false, true, Instant.EPOCH),
                null);
    }
}
