package ru.krotarnya.diasync2.common;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.Test;

public class TrendCalculatorTest {
    private static final Instant LATEST_TIME = Instant.parse("2026-08-27T12:00:00Z");
    private final TrendCalculator calculator = new TrendCalculator();

    @Test
    public void returnsPlaceholderWithoutPreviousPointInWindow() {
        assertEquals("", calculator.calculate(List.of(point(LATEST_TIME, 100.0)), true));
        assertEquals("", calculator.calculate(List.of(
                point(LATEST_TIME, 100.0),
                point(LATEST_TIME.minusSeconds(601), 80.0)), true));
    }

    @Test
    public void mapsTrendBoundariesFromAverageOfPreviousTenMinutes() {
        assertTrend("⇊", 86.5);
        assertTrend("↓", 87.0);
        assertTrend("↘", 93.1);
        assertTrend("→", 97.1);
        assertTrend("↗", 103.1);
        assertTrend("↑", 107.1);
        assertTrend("⇈", 113.6);
    }

    @Test
    public void usesCalibratedDisplayValuesWhenEnabled() {
        SensorPoint latest = new SensorPoint(
                LATEST_TIME,
                new GlucoseValue(100.0),
                "sensor",
                new Calibration(2.0, 0.0));
        SensorPoint previous = new SensorPoint(
                LATEST_TIME.minusSeconds(60),
                new GlucoseValue(95.0),
                "sensor",
                null);

        assertEquals("⇈", calculator.calculate(List.of(latest, previous), true));
        assertEquals("↗", calculator.calculate(List.of(latest, previous), false));
    }

    private void assertTrend(String expected, double latestMgDl) {
        assertEquals(expected, calculator.calculate(List.of(
                point(LATEST_TIME, latestMgDl),
                point(LATEST_TIME.minusSeconds(60), 100.0),
                point(LATEST_TIME.minusSeconds(300), 100.0)), true));
    }

    private SensorPoint point(Instant timestamp, double mgDl) {
        return new SensorPoint(timestamp, new GlucoseValue(mgDl), "sensor", null);
    }
}
