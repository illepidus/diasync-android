package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import ru.krotarnya.diasync2.common.Calibration;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.GlucoseValue;
import ru.krotarnya.diasync2.common.SensorPoint;
import ru.krotarnya.diasync2.common.TrendCalculator;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;
import ru.krotarnya.diasync2.common.wear.WearSnapshotCodec;
import ru.krotarnya.diasync2.settings.AlertSettings;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.GraphWindow;
import ru.krotarnya.diasync2.settings.WatchSettings;

public class WearSnapshotBuilderTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

    @Test
    public void buildsBoundedCredentialFreeSnapshotWithDisplayContract() {
        AtomicReference<Instant> requestedFrom = new AtomicReference<>();
        AtomicInteger requestedLimit = new AtomicInteger();
        List<DataPoint> points = List.of(
                point(NOW.minusSeconds(60), 120.0, new Calibration(1.1, -2.0)),
                point(NOW.minusSeconds(6 * 60), 110.0, null));
        WearSnapshotBuilder builder = new WearSnapshotBuilder(
                (userId, from, limit) -> {
                    assertEquals("secret-user-id", userId);
                    requestedFrom.set(from);
                    requestedLimit.set(limit);
                    return points;
                },
                new TrendCalculator(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        AppConfiguration configuration = configuration();
        WatchSettings display = new WatchSettings(
                GraphWindow.THREE_HOURS,
                false,
                false,
                true);

        WearSnapshot snapshot = builder.build(
                configuration,
                display,
                new AlertSettings(true, false, true),
                Instant.EPOCH,
                null);

        assertEquals(
                NOW.minus(WearSnapshot.MAX_GRAPH_WINDOW)
                        .minus(WearSnapshot.GRAPH_WINDOW_MARGIN),
                requestedFrom.get());
        assertEquals(WearSnapshot.MAX_POINTS, requestedLimit.get());
        assertEquals(2, snapshot.points().size());
        assertEquals(1.1, snapshot.points().get(0).calibrationSlope(), 0.0);
        assertEquals(180, snapshot.display().graphWindowMinutes());
        assertEquals(GlucoseUnit.MMOL_L, snapshot.display().unit());
        assertEquals(70.0, snapshot.display().lowMgDl(), 0.0);
        assertTrue(snapshot.display().useCalibration());
        assertFalse(snapshot.display().graphZones());
        assertEquals("⇈", snapshot.display().trend());
        String json = new String(new WearSnapshotCodec().encode(snapshot), StandardCharsets.UTF_8);
        assertFalse(json.contains(configuration.userId()));
        assertFalse(json.contains(configuration.baseUrl()));
        assertFalse(json.contains("sensor-private-value"));
    }

    private AppConfiguration configuration() {
        return new AppConfiguration(
                "https://backend.example",
                "secret-user-id",
                GlucoseUnit.MMOL_L,
                true,
                70.0,
                180.0,
                GraphWindow.THREE_HOURS,
                true,
                false,
                true);
    }

    private DataPoint point(Instant timestamp, double mgDl, Calibration calibration) {
        return new DataPoint(
                timestamp,
                timestamp,
                new SensorPoint(
                        timestamp,
                        new GlucoseValue(mgDl),
                        "sensor-private-value",
                        calibration),
                null,
                null,
                null);
    }
}
