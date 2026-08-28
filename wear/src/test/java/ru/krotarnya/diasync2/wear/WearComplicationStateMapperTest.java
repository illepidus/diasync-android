package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
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
    private final WearComplicationStateMapper mapper = new WearComplicationStateMapper();

    @Test
    public void missingSnapshotMapsToVisibleNoData() {
        WearComplicationState state = mapper.map(Optional.empty());

        assertEquals("NO DATA", state.text());
        assertEquals("No glucose data", state.contentDescription());
    }

    @Test
    public void emptySnapshotMapsToVisibleNoData() {
        WearComplicationState state = mapper.map(Optional.of(snapshot(
                GlucoseUnit.MMOL_L, true, "→", List.of())));

        assertEquals("NO DATA", state.text());
    }

    @Test
    public void latestCalibratedValueAndTrendMapToShortText() {
        WearGlucosePoint latest = new WearGlucosePoint(
                NOW.minusSeconds(30), 108.0, 1.1, 1.2);

        WearComplicationState state = mapper.map(Optional.of(snapshot(
                GlucoseUnit.MMOL_L, true, "→", List.of(latest))));

        assertEquals("6.7 →", state.text());
        assertEquals("mmol/L", state.title());
        assertEquals("Glucose 6.7 mmol/L, trend →", state.contentDescription());
    }

    @Test
    public void disabledTrendArrowMapsOnlyValueInConfiguredUnit() {
        WearGlucosePoint latest = new WearGlucosePoint(
                NOW.minusSeconds(30), 181.0, null, null);

        WearComplicationState state = mapper.map(Optional.of(snapshot(
                GlucoseUnit.MG_DL, false, "↑", List.of(latest))));

        assertEquals("181", state.text());
        assertEquals("mg/dL", state.title());
    }

    private WearSnapshot snapshot(
            GlucoseUnit unit,
            boolean trendArrow,
            String trend,
            List<WearGlucosePoint> points) {
        return new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                NOW,
                points,
                new WearDisplayPolicy(
                        unit,
                        true,
                        70.0,
                        180.0,
                        30,
                        true,
                        false,
                        trendArrow,
                        trend),
                new WearAlertPolicy(false, false, true, Instant.EPOCH),
                null);
    }
}
