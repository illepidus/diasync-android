package ru.krotarnya.diasync2.common.master;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

public class XdripEventCodecTest {
    private static final List<String> INVALID_FIXTURES = List.of(
            "unsupported-version.json",
            "multiple-subtypes.json",
            "mismatched-event-id.json",
            "sensor-without-calibration.json",
            "manual-with-calibration.json",
            "carbs-with-calibration.json",
            "unknown-field.json");

    private final XdripEventCodec codec = new XdripEventCodec();

    @Test
    public void sensorFixturePreservesRawValueAndCalibration() throws IOException {
        byte[] fixture = fixture("valid/sensor.json");

        XdripEvent event = codec.decode(fixture);

        assertEquals(XdripEventType.SENSOR, event.eventType());
        assertEquals(123.0, event.sensor().rawValue(), 0.0);
        assertEquals("libre-sensor-id", event.sensor().sensorId());
        assertEquals(1.1, event.sensor().calibration().slope(), 0.0);
        assertEquals(-2.0, event.sensor().calibration().intercept(), 0.0);
        assertNull(event.manualGlucose());
        assertNull(event.carbs());
        assertMatchesGoldenJson(fixture, event);
    }

    @Test
    public void identityCalibrationIsExplicitInEncodedSensorEvent() {
        XdripEvent event = new XdripEvent(
                XdripEvent.PROTOCOL_VERSION,
                "SENSOR:550e8400-e29b-41d4-a716-446655440003",
                XdripEventType.SENSOR,
                1789027200000L,
                new XdripEvent.Sensor(
                        123.0,
                        "libre-sensor-id",
                        XdripEvent.Calibration.identity()),
                null,
                null);

        String json = new String(codec.encode(event), StandardCharsets.UTF_8);

        assertEquals(
                "{\"protocolVersion\":1,"
                        + "\"eventId\":\"SENSOR:550e8400-e29b-41d4-a716-446655440003\","
                        + "\"eventType\":\"SENSOR\","
                        + "\"occurredAtEpochMillis\":1789027200000,"
                        + "\"sensor\":{\"rawValue\":123.0,\"sensorId\":\"libre-sensor-id\","
                        + "\"calibration\":{\"slope\":1.0,\"intercept\":0.0}}}",
                json);
    }

    @Test
    public void manualFixtureHasNoCalibration() throws IOException {
        byte[] fixture = fixture("valid/manual-glucose.json");

        XdripEvent event = codec.decode(fixture);

        assertEquals(XdripEventType.MANUAL_GLUCOSE, event.eventType());
        assertEquals(121.0, event.manualGlucose().mgdl(), 0.0);
        assertNull(event.sensor());
        assertNull(event.carbs());
        assertMatchesGoldenJson(fixture, event);
    }

    @Test
    public void carbsFixtureHasNoCalibration() throws IOException {
        byte[] fixture = fixture("valid/carbs.json");

        XdripEvent event = codec.decode(fixture);

        assertEquals(XdripEventType.CARBS, event.eventType());
        assertEquals(20.0, event.carbs().grams(), 0.0);
        assertEquals("Afternoon snack", event.carbs().description());
        assertNull(event.sensor());
        assertNull(event.manualGlucose());
        assertMatchesGoldenJson(fixture, event);
    }

    @Test
    public void rejectsAllInvalidGoldenFixtures() {
        for (String fixture : INVALID_FIXTURES) {
            assertThrows(fixture, XdripProtocolException.class,
                    () -> codec.decode(fixture("invalid/" + fixture)));
        }
    }

    @Test
    public void rejectsOversizedPayload() {
        assertThrows(XdripProtocolException.class,
                () -> codec.decode(new byte[XdripEvent.MAX_PAYLOAD_BYTES + 1]));
    }

    @Test
    public void rejectsMalformedUtf8() {
        assertThrows(XdripProtocolException.class,
                () -> codec.decode(new byte[] {(byte) 0xc3, (byte) 0x28}));
    }

    @Test
    public void canonicalizationRemovesInsignificantFormatting() {
        byte[] formatted = """
                {
                  "protocolVersion": 1,
                  "eventId": "CARBS:550e8400-e29b-41d4-a716-446655440002",
                  "eventType": "CARBS",
                  "occurredAtEpochMillis": 1789027320000,
                  "carbs": {"grams": 20.0, "description": "Afternoon snack"}
                }
                """.getBytes(StandardCharsets.UTF_8);

        assertEquals(
                new String(fixtureUnchecked("valid/carbs.json"), StandardCharsets.UTF_8).strip(),
                new String(codec.canonicalize(formatted), StandardCharsets.UTF_8));
    }

    @Test
    public void rejectsCalibrationOnNonSensorModels() {
        assertThrows(IllegalArgumentException.class, () -> new XdripEvent(
                1,
                "MANUAL_GLUCOSE:550e8400-e29b-41d4-a716-446655440001",
                XdripEventType.MANUAL_GLUCOSE,
                1789027260000L,
                new XdripEvent.Sensor(123.0, "sensor", XdripEvent.Calibration.identity()),
                new XdripEvent.ManualGlucose(121.0),
                null));
    }

    private static byte[] fixture(String name) throws IOException {
        String path = "/xdrip-event-v1/" + name;
        try (InputStream input = XdripEventCodecTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing fixture: " + path);
            }
            return input.readAllBytes();
        }
    }

    private static byte[] fixtureUnchecked(String name) {
        try {
            return fixture(name);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private void assertMatchesGoldenJson(byte[] fixture, XdripEvent event) {
        assertEquals(
                new String(fixture, StandardCharsets.UTF_8).strip(),
                new String(codec.encode(event), StandardCharsets.UTF_8));
    }
}
