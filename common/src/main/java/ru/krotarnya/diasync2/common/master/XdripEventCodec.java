package ru.krotarnya.diasync2.common.master;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class XdripEventCodec {
    private static final Set<String> ROOT_FIELDS = Set.of(
            "protocolVersion",
            "eventId",
            "eventType",
            "occurredAtEpochMillis",
            "sensor",
            "manualGlucose",
            "carbs");
    private static final Set<String> SENSOR_FIELDS = Set.of("rawValue", "sensorId", "calibration");
    private static final Set<String> CALIBRATION_FIELDS = Set.of("slope", "intercept");
    private static final Set<String> MANUAL_FIELDS = Set.of("mgdl");
    private static final Set<String> CARBS_FIELDS = Set.of("grams", "description");

    private final Gson gson = new Gson();

    public byte[] encode(XdripEvent event) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("protocolVersion", event.protocolVersion());
            root.addProperty("eventId", event.eventId());
            root.addProperty("eventType", event.eventType().name());
            root.addProperty("occurredAtEpochMillis", event.occurredAtEpochMillis());
            switch (event.eventType()) {
                case SENSOR -> root.add("sensor", encodeSensor(event.sensor()));
                case MANUAL_GLUCOSE -> root.add("manualGlucose", encodeManual(event.manualGlucose()));
                case CARBS -> root.add("carbs", encodeCarbs(event.carbs()));
            }
            byte[] payload = gson.toJson(root).getBytes(StandardCharsets.UTF_8);
            requirePayload(payload);
            return payload;
        } catch (XdripProtocolException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new XdripProtocolException("xDrip event cannot be encoded", exception);
        }
    }

    public XdripEvent decode(byte[] payload) {
        requirePayload(payload);
        try {
            JsonElement parsed = com.google.gson.JsonParser.parseString(decodeUtf8(payload));
            JsonObject root = requireObject(parsed, "event");
            requireOnlyFields(root, ROOT_FIELDS, "event");

            int protocolVersion = requiredInt(root, "protocolVersion");
            String eventId = requiredString(root, "eventId");
            XdripEventType eventType = XdripEventType.valueOf(requiredString(root, "eventType"));
            long occurredAt = requiredLong(root, "occurredAtEpochMillis");
            XdripEvent.Sensor sensor = optionalObject(root, "sensor") == null
                    ? null
                    : decodeSensor(optionalObject(root, "sensor"));
            XdripEvent.ManualGlucose manual = optionalObject(root, "manualGlucose") == null
                    ? null
                    : decodeManual(optionalObject(root, "manualGlucose"));
            XdripEvent.Carbs carbs = optionalObject(root, "carbs") == null
                    ? null
                    : decodeCarbs(optionalObject(root, "carbs"));
            return new XdripEvent(
                    protocolVersion,
                    eventId,
                    eventType,
                    occurredAt,
                    sensor,
                    manual,
                    carbs);
        } catch (XdripProtocolException exception) {
            throw exception;
        } catch (JsonParseException | IllegalArgumentException | IllegalStateException exception) {
            throw new XdripProtocolException("xDrip event is invalid", exception);
        }
    }

    public byte[] canonicalize(byte[] payload) {
        return encode(decode(payload));
    }

    private static JsonObject encodeSensor(XdripEvent.Sensor sensor) {
        JsonObject json = new JsonObject();
        json.addProperty("rawValue", sensor.rawValue());
        json.addProperty("sensorId", sensor.sensorId());
        JsonObject calibration = new JsonObject();
        calibration.addProperty("slope", sensor.calibration().slope());
        calibration.addProperty("intercept", sensor.calibration().intercept());
        json.add("calibration", calibration);
        return json;
    }

    private static JsonObject encodeManual(XdripEvent.ManualGlucose manual) {
        JsonObject json = new JsonObject();
        json.addProperty("mgdl", manual.mgdl());
        return json;
    }

    private static JsonObject encodeCarbs(XdripEvent.Carbs carbs) {
        JsonObject json = new JsonObject();
        json.addProperty("grams", carbs.grams());
        if (carbs.description() != null) {
            json.addProperty("description", carbs.description());
        }
        return json;
    }

    private static XdripEvent.Sensor decodeSensor(JsonObject json) {
        requireOnlyFields(json, SENSOR_FIELDS, "sensor");
        JsonObject calibration = requiredObject(json, "calibration");
        requireOnlyFields(calibration, CALIBRATION_FIELDS, "calibration");
        return new XdripEvent.Sensor(
                requiredDouble(json, "rawValue"),
                requiredString(json, "sensorId"),
                new XdripEvent.Calibration(
                        requiredDouble(calibration, "slope"),
                        requiredDouble(calibration, "intercept")));
    }

    private static XdripEvent.ManualGlucose decodeManual(JsonObject json) {
        requireOnlyFields(json, MANUAL_FIELDS, "manualGlucose");
        return new XdripEvent.ManualGlucose(requiredDouble(json, "mgdl"));
    }

    private static XdripEvent.Carbs decodeCarbs(JsonObject json) {
        requireOnlyFields(json, CARBS_FIELDS, "carbs");
        return new XdripEvent.Carbs(
                requiredDouble(json, "grams"),
                optionalString(json, "description"));
    }

    private static void requirePayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            throw new XdripProtocolException("xDrip event payload is empty");
        }
        if (payload.length > XdripEvent.MAX_PAYLOAD_BYTES) {
            throw new XdripProtocolException("xDrip event payload is too large");
        }
    }

    private static String decodeUtf8(byte[] payload) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(payload))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new XdripProtocolException("xDrip event payload is not valid UTF-8", exception);
        }
    }

    private static void requireOnlyFields(JsonObject json, Set<String> allowed, String objectName) {
        for (String field : json.keySet()) {
            if (!allowed.contains(field)) {
                throw new XdripProtocolException("Unknown field in " + objectName + ": " + field);
            }
        }
    }

    private static JsonObject requireObject(JsonElement value, String name) {
        if (value == null || value.isJsonNull() || !value.isJsonObject()) {
            throw new XdripProtocolException("Invalid object: " + name);
        }
        return value.getAsJsonObject();
    }

    private static JsonObject requiredObject(JsonObject json, String name) {
        return requireObject(json.get(name), name);
    }

    private static JsonObject optionalObject(JsonObject json, String name) {
        JsonElement value = json.get(name);
        return value == null || value.isJsonNull() ? null : requireObject(value, name);
    }

    private static JsonElement requiredPrimitive(JsonObject json, String name) {
        JsonElement value = json.get(name);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            throw new XdripProtocolException("Invalid field: " + name);
        }
        return value;
    }

    private static String requiredString(JsonObject json, String name) {
        JsonElement value = requiredPrimitive(json, name);
        if (!value.getAsJsonPrimitive().isString()) {
            throw new XdripProtocolException("Invalid string: " + name);
        }
        return value.getAsString();
    }

    private static String optionalString(JsonObject json, String name) {
        JsonElement value = json.get(name);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new XdripProtocolException("Invalid string: " + name);
        }
        return value.getAsString();
    }

    private static double requiredDouble(JsonObject json, String name) {
        JsonElement value = requiredPrimitive(json, name);
        if (!value.getAsJsonPrimitive().isNumber()) {
            throw new XdripProtocolException("Invalid number: " + name);
        }
        return value.getAsDouble();
    }

    private static int requiredInt(JsonObject json, String name) {
        long value = requiredLong(json, name);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new XdripProtocolException("Integer is out of range: " + name);
        }
        return (int) value;
    }

    private static long requiredLong(JsonObject json, String name) {
        JsonElement value = requiredPrimitive(json, name);
        if (!value.getAsJsonPrimitive().isNumber()) {
            throw new XdripProtocolException("Invalid integer: " + name);
        }
        String text = value.getAsString();
        if (!text.matches("-?(0|[1-9][0-9]*)")) {
            throw new XdripProtocolException("Invalid integer: " + name);
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            throw new XdripProtocolException("Integer is out of range: " + name, exception);
        }
    }
}
