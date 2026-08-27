package ru.krotarnya.diasync2.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.Test;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.GlucoseValue;
import ru.krotarnya.diasync2.common.SensorPoint;
import ru.krotarnya.diasync2.common.TrendCalculator;

public class WidgetPresenterTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private final WidgetPresenter presenter = new WidgetPresenter(
            Clock.fixed(NOW, ZoneOffset.UTC),
            new TrendCalculator());

    @Test
    public void mapsNoDataState() {
        WidgetState state = presenter.present(List.of(), GlucoseUnit.MMOL_L, true);

        assertEquals("----", state.value());
        assertEquals("-", state.trend());
        assertEquals("NO DATA", state.message());
        assertTrue(state.valueVisible());
        assertFalse(state.strikeThrough());
    }

    @Test
    public void keepsMessageEmptyUntilOneMinute() {
        WidgetState state = presentAt(NOW.minusSeconds(59), 100.0);

        assertEquals("", state.message());
        assertFalse(state.strikeThrough());
    }

    @Test
    public void showsSingularAndPluralMinuteAge() {
        assertEquals("1 minute ago", presentAt(NOW.minusSeconds(60), 100.0).message());
        assertEquals("2 minutes ago", presentAt(NOW.minusSeconds(179), 100.0).message());
    }

    @Test
    public void strikesValueOnlyAfterTenMinutes() {
        assertFalse(presentAt(NOW.minusSeconds(600), 100.0).strikeThrough());
        assertTrue(presentAt(NOW.minusSeconds(601), 100.0).strikeThrough());
    }

    @Test
    public void hidesValueForTimestampMoreThanOneMinuteInFuture() {
        WidgetState tolerated = presentAt(NOW.plusSeconds(60), 100.0);
        WidgetState future = presentAt(NOW.plusSeconds(61), 100.0);

        assertTrue(tolerated.valueVisible());
        assertFalse(future.valueVisible());
        assertEquals("DATA FROM FAR FUTURE", future.message());
    }

    @Test
    public void mapsRangeColorAtInclusiveThresholds() {
        assertEquals(WidgetState.Range.LOW, presentAt(NOW, 70.0).range());
        assertEquals(WidgetState.Range.NORMAL, presentAt(NOW, 70.1).range());
        assertEquals(WidgetState.Range.NORMAL, presentAt(NOW, 179.9).range());
        assertEquals(WidgetState.Range.HIGH, presentAt(NOW, 180.0).range());
    }

    @Test
    public void formatsUnitCalibrationAndComputedTrend() {
        SensorPoint latest = sensor(NOW, 100.0);
        SensorPoint previous = sensor(NOW.minusSeconds(60), 90.0);

        WidgetState state = presenter.present(
                List.of(data(latest), data(previous)),
                GlucoseUnit.MMOL_L,
                true);

        assertEquals("5.6", state.value());
        assertEquals("mmol/L", state.unit());
        assertEquals("↑", state.trend());
    }

    private WidgetState presentAt(Instant timestamp, double mgDl) {
        return presenter.present(
                List.of(data(sensor(timestamp, mgDl))),
                GlucoseUnit.MG_DL,
                true);
    }

    private DataPoint data(SensorPoint sensorPoint) {
        return new DataPoint(
                sensorPoint.timestamp(),
                null,
                sensorPoint,
                null,
                null,
                null);
    }

    private SensorPoint sensor(Instant timestamp, double mgDl) {
        return new SensorPoint(timestamp, new GlucoseValue(mgDl), "sensor", null);
    }
}
