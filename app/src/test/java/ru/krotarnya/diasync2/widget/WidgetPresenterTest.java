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
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.GraphWindow;

public class WidgetPresenterTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private final WidgetPresenter presenter = new WidgetPresenter(
            Clock.fixed(NOW, ZoneOffset.UTC),
            new TrendCalculator());

    @Test
    public void mapsNoDataState() {
        WidgetState state = presenter.present(List.of(), configuration(GlucoseUnit.MMOL_L));

        assertEquals("----", state.value());
        assertEquals("-", state.trend());
        assertEquals("NO DATA", state.message());
        assertTrue(state.valueVisible());
        assertFalse(state.strikeThrough());
        assertFalse(state.trendVisible());
        assertFalse(state.graphVisible());
    }

    @Test
    public void keepsMessageEmptyUntilTwoMinutes() {
        assertEquals("", presentAt(NOW.minusSeconds(119), 100.0).message());
        assertFalse(presentAt(NOW.minusSeconds(119), 100.0).strikeThrough());
    }

    @Test
    public void showsCompactMinuteAgeFromTwoMinutes() {
        assertEquals("2m ago", presentAt(NOW.minusSeconds(120), 100.0).message());
        assertEquals("2m ago", presentAt(NOW.minusSeconds(179), 100.0).message());
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
        assertFalse(future.graphVisible());
    }

    @Test
    public void mapsRangeColorAtInclusiveThresholds() {
        assertEquals(WidgetState.Range.LOW, presentAt(NOW, 80.0, 80.0, 160.0).range());
        assertEquals(WidgetState.Range.NORMAL, presentAt(NOW, 80.1, 80.0, 160.0).range());
        assertEquals(WidgetState.Range.NORMAL, presentAt(NOW, 159.9, 80.0, 160.0).range());
        assertEquals(WidgetState.Range.HIGH, presentAt(NOW, 160.0, 80.0, 160.0).range());
    }

    @Test
    public void formatsUnitCalibrationAndComputedTrend() {
        SensorPoint latest = sensor(NOW, 100.0);
        SensorPoint previous = sensor(NOW.minusSeconds(60), 90.0);

        WidgetState state = presenter.present(
                List.of(data(latest), data(previous)),
                configuration(GlucoseUnit.MMOL_L));

        assertEquals("5.6", state.value());
        assertEquals("↑", state.trend());
        assertEquals(2, state.graphSamples().size());
        assertTrue(state.graphVisible());
    }

    private WidgetState presentAt(Instant timestamp, double mgDl) {
        return presentAt(timestamp, mgDl, 70.0, 180.0);
    }

    private WidgetState presentAt(
            Instant timestamp,
            double mgDl,
            double lowMgDl,
            double highMgDl
    ) {
        return presenter.present(
                List.of(data(sensor(timestamp, mgDl))),
                configuration(GlucoseUnit.MG_DL, lowMgDl, highMgDl));
    }

    private AppConfiguration configuration(GlucoseUnit unit) {
        return configuration(unit, 70.0, 180.0);
    }

    private AppConfiguration configuration(GlucoseUnit unit, double lowMgDl, double highMgDl) {
        return new AppConfiguration(
                "https://example.test",
                "secret",
                unit,
                true,
                lowMgDl,
                highMgDl,
                GraphWindow.THIRTY_MINUTES,
                true,
                false,
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
