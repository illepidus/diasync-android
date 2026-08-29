package ru.krotarnya.diasync2.common.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import ru.krotarnya.diasync2.common.AlertType;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public class WearSnapshotCodecTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");
    private final WearSnapshotCodec codec = new WearSnapshotCodec();

    @Test
    public void serializationMatchesGoldenJson() {
        String json = new String(codec.encode(snapshot()), StandardCharsets.UTF_8);

        assertEquals(
                "{\"v\":1,\"generatedAt\":\"2026-08-28T12:00:00Z\","
                        + "\"points\":[{\"timestamp\":\"2026-08-28T11:59:00Z\","
                        + "\"rawMgDl\":123.0,\"calibrationSlope\":1.1,"
                        + "\"calibrationIntercept\":-2.0}],"
                        + "\"display\":{\"unit\":\"MMOL_L\",\"useCalibration\":true,"
                        + "\"lowMgDl\":70.0,\"highMgDl\":180.0,"
                        + "\"graphWindowMinutes\":180,\"graphZones\":true,"
                        + "\"graphLines\":false,\"trendArrow\":true,\"trend\":\"↗\"},"
                        + "\"alerts\":{\"lowEnabled\":true,\"highEnabled\":false,"
                        + "\"noDataEnabled\":true,\"snoozedUntil\":\"1970-01-01T00:00:00Z\"},"
                        + "\"alertEvent\":{\"eventId\":\"LOW:2026-08-28T11:59:00Z\","
                        + "\"type\":\"LOW\",\"measurementTimestamp\":"
                        + "\"2026-08-28T11:59:00Z\",\"generatedAt\":"
                        + "\"2026-08-28T11:59:00Z\",\"expiresAt\":"
                        + "\"2026-08-28T12:01:00Z\"}}",
                json);
    }

    @Test
    public void roundTripPreservesTimestampsThresholdsAndTrend() {
        WearSnapshot decoded = codec.decode(codec.encode(snapshot()));

        assertEquals(NOW, decoded.generatedAt());
        assertEquals(NOW.minusSeconds(60), decoded.points().get(0).timestamp());
        assertEquals(70.0, decoded.display().lowMgDl(), 0.0);
        assertEquals(180.0, decoded.display().highMgDl(), 0.0);
        assertEquals("↗", decoded.display().trend());
        assertEquals(NOW.minusSeconds(60), decoded.alertEvent().measurementTimestamp());
        assertEquals(NOW.minusSeconds(60), decoded.alertEvent().generatedAt());
    }

    @Test
    public void rejectsOldNewAndCorruptPayloads() {
        byte[] valid = codec.encode(snapshot());
        String json = new String(valid, StandardCharsets.UTF_8);

        assertThrows(WearProtocolException.class, () -> codec.decode(
                json.replace("\"v\":1", "\"v\":0").getBytes(StandardCharsets.UTF_8)));
        assertThrows(WearProtocolException.class, () -> codec.decode(
                json.replace("\"v\":1", "\"v\":2").getBytes(StandardCharsets.UTF_8)));
        assertThrows(WearProtocolException.class, () -> codec.decode("{".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void wireShapeCannotContainPhoneCredentials() {
        String json = new String(codec.encode(snapshot()), StandardCharsets.UTF_8);

        assertFalse(json.contains("secret-user-id"));
        assertFalse(json.contains("https://backend.example"));
        assertFalse(json.contains("userId"));
        assertFalse(json.contains("baseUrl"));
    }

    @Test
    public void rejectsPointsOutsideWindowAndOversizedPayload() {
        WearSnapshot source = snapshot();
        assertThrows(IllegalArgumentException.class, () -> new WearSnapshot(
                1,
                NOW,
                List.of(new WearGlucosePoint(
                        NOW.minus(WearSnapshot.MAX_GRAPH_WINDOW)
                                .minus(WearSnapshot.GRAPH_WINDOW_MARGIN)
                                .minusSeconds(1),
                        100.0,
                        null,
                        null)),
                source.display(),
                source.alerts(),
                null));
        assertThrows(WearProtocolException.class, () -> codec.decode(
                new byte[WearSnapshot.MAX_PAYLOAD_BYTES + 1]));
    }

    @Test
    public void alertEventExpiryAndDedupeAreStable() {
        WearAlertEvent event = WearAlertEvent.create(AlertType.LOW, NOW.minusSeconds(60));

        assertEquals("LOW:2026-08-28T11:59:00Z", event.eventId());
        assertTrue(event.shouldHandle(null, NOW));
        assertFalse(event.shouldHandle(event.eventId(), NOW));
        assertFalse(event.shouldHandle(null, event.expiresAt()));
        assertFalse(event.shouldHandle(null, event.generatedAt().minusMillis(1)));
    }

    private WearSnapshot snapshot() {
        Instant measurement = NOW.minusSeconds(60);
        return new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                NOW,
                List.of(new WearGlucosePoint(measurement, 123.0, 1.1, -2.0)),
                new WearDisplayPolicy(
                        GlucoseUnit.MMOL_L,
                        true,
                        70.0,
                        180.0,
                        180,
                        true,
                        false,
                        true,
                        "↗"),
                new WearAlertPolicy(true, false, true, Instant.EPOCH),
                WearAlertEvent.create(AlertType.LOW, measurement));
    }
}
