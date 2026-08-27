package ru.krotarnya.diasync2.common;

import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Test;

public class GlucoseValueTest {
    @Test
    public void convertsAndFormatsBothUnits() {
        GlucoseValue value = new GlucoseValue(180.0);

        assertEquals(180.0, value.in(GlucoseUnit.MG_DL), 0.0001);
        assertEquals(10.0, value.in(GlucoseUnit.MMOL_L), 0.0001);
        assertEquals("180", value.format(GlucoseUnit.MG_DL));
        assertEquals("10.0", value.format(GlucoseUnit.MMOL_L));
        assertEquals(180.0, GlucoseUnit.MMOL_L.toMgDl(10.0), 0.0001);
    }

    @Test
    public void appliesCalibrationOnlyWhenRequested() {
        SensorPoint point = new SensorPoint(
                Instant.parse("2026-08-27T10:00:00Z"),
                new GlucoseValue(100.0),
                "sensor",
                new Calibration(1.1, 5.0));

        assertEquals(100.0, point.displayValue(false).mgDl(), 0.0001);
        assertEquals(115.0, point.displayValue(true).mgDl(), 0.0001);
    }

    @Test
    public void calculatesAgeUsingInjectedClock() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-27T10:05:30Z"), ZoneOffset.UTC);

        assertEquals(
                Duration.ofMinutes(5).plusSeconds(30),
                DataAge.at(Instant.parse("2026-08-27T10:00:00Z"), clock));
    }
}
