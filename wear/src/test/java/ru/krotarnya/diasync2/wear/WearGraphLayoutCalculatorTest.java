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

public class WearGraphLayoutCalculatorTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");
    private final WearGraphLayoutCalculator calculator = new WearGraphLayoutCalculator();

    @Test
    public void everyPointAndThresholdStayWithinBitmapGraphBounds() {
        for (int window : List.of(30, 60, 180)) {
            WearComplicationState state = state(window);

            for (int[] size : List.of(new int[]{360, 235}, new int[]{320, 209})) {
                WearGraphLayout layout = calculator.calculate(state, size[0], size[1]);

                assertEquals(3, layout.points().size());
                assertTrue(layout.bounds().contains(layout.bounds().left(), layout.lowY()));
                assertTrue(layout.bounds().contains(layout.bounds().right(), layout.highY()));
                for (WearGraphLayout.Point point : layout.points()) {
                    assertTrue(layout.bounds().contains(point.x(), point.y()));
                }
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroSizedBitmap() {
        calculator.calculate(state(30), 0, 235);
    }

    private WearComplicationState state(int window) {
        WearSnapshot snapshot = new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                NOW,
                List.of(
                        new WearGlucosePoint(NOW.minusSeconds(30), 200.0, null, null),
                        new WearGlucosePoint(NOW.minusSeconds(window * 20L), 110.0, null, null),
                        new WearGlucosePoint(NOW.minusSeconds(window * 50L), 60.0, null, null)),
                new WearDisplayPolicy(
                        GlucoseUnit.MG_DL,
                        true,
                        70.0,
                        180.0,
                        window,
                        true,
                        true,
                        true,
                        "↑"),
                new WearAlertPolicy(false, false, false, Instant.EPOCH),
                null);
        return new WearComplicationStateMapper(Clock.fixed(NOW, ZoneOffset.UTC))
                .map(Optional.of(snapshot));
    }
}
