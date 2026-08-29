package ru.krotarnya.diasync2.common.wear;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import ru.krotarnya.diasync2.common.AlertType;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public final class WearSnapshotCodec {
    private final Gson gson = new Gson();

    public byte[] encode(WearSnapshot snapshot) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("v", snapshot.protocolVersion());
            root.addProperty("generatedAt", snapshot.generatedAt().toString());
            JsonArray points = getJsonArray(snapshot);
            root.add("points", points);
            root.add("display", encodeDisplay(snapshot.display()));
            root.add("alerts", encodeAlerts(snapshot.alerts()));
            if (snapshot.alertEvent() != null) {
                root.add("alertEvent", encodeEvent(snapshot.alertEvent()));
            }
            byte[] payload = gson.toJson(root).getBytes(StandardCharsets.UTF_8);
            requireSize(payload);
            return payload;
        } catch (WearProtocolException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new WearProtocolException("Snapshot cannot be encoded", exception);
        }
    }

    private static JsonArray getJsonArray(WearSnapshot snapshot) {
        JsonArray points = new JsonArray();
        for (WearGlucosePoint point : snapshot.points()) {
            JsonObject encodedPoint = new JsonObject();
            encodedPoint.addProperty("timestamp", point.timestamp().toString());
            encodedPoint.addProperty("rawMgDl", point.rawMgDl());
            if (point.calibrationSlope() != null) {
                encodedPoint.addProperty("calibrationSlope", point.calibrationSlope());
                encodedPoint.addProperty("calibrationIntercept", point.calibrationIntercept());
            }
            points.add(encodedPoint);
        }
        return points;
    }

    public WearSnapshot decode(byte[] payload) {
        requireSize(payload);
        try {
            JsonObject root = gson.fromJson(
                    new String(payload, StandardCharsets.UTF_8),
                    JsonObject.class);
            if (root == null) {
                throw new WearProtocolException("Snapshot is empty");
            }
            int version = required(root, "v").getAsInt();
            if (version != WearSnapshot.PROTOCOL_VERSION) {
                throw new WearProtocolException("Protocol version is unsupported");
            }
            Instant generatedAt = Instant.parse(required(root, "generatedAt").getAsString());
            List<WearGlucosePoint> points = decodePoints(required(root, "points").getAsJsonArray());
            WearDisplayPolicy display = decodeDisplay(required(root, "display").getAsJsonObject());
            WearAlertPolicy alerts = decodeAlerts(required(root, "alerts").getAsJsonObject());
            WearAlertEvent event = root.has("alertEvent")
                    ? decodeEvent(root.getAsJsonObject("alertEvent"))
                    : null;
            return new WearSnapshot(version, generatedAt, points, display, alerts, event);
        } catch (WearProtocolException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new WearProtocolException("Snapshot is invalid", exception);
        }
    }

    private JsonObject encodeDisplay(WearDisplayPolicy display) {
        JsonObject json = new JsonObject();
        json.addProperty("unit", display.unit().name());
        json.addProperty("useCalibration", display.useCalibration());
        json.addProperty("lowMgDl", display.lowMgDl());
        json.addProperty("highMgDl", display.highMgDl());
        json.addProperty("graphWindowMinutes", display.graphWindowMinutes());
        json.addProperty("graphZones", display.graphZones());
        json.addProperty("graphLines", display.graphLines());
        json.addProperty("trendArrow", display.trendArrow());
        json.addProperty("trend", display.trend());
        return json;
    }

    private JsonObject encodeAlerts(WearAlertPolicy alerts) {
        JsonObject json = new JsonObject();
        json.addProperty("lowEnabled", alerts.lowEnabled());
        json.addProperty("highEnabled", alerts.highEnabled());
        json.addProperty("noDataEnabled", alerts.noDataEnabled());
        json.addProperty("snoozedUntil", alerts.snoozedUntil().toString());
        return json;
    }

    private JsonObject encodeEvent(WearAlertEvent event) {
        JsonObject json = new JsonObject();
        json.addProperty("eventId", event.eventId());
        json.addProperty("type", event.type().name());
        json.addProperty("measurementTimestamp", event.measurementTimestamp().toString());
        json.addProperty("generatedAt", event.generatedAt().toString());
        json.addProperty("expiresAt", event.expiresAt().toString());
        return json;
    }

    private List<WearGlucosePoint> decodePoints(JsonArray json) {
        List<WearGlucosePoint> points = new ArrayList<>(json.size());
        for (JsonElement element : json) {
            JsonObject point = element.getAsJsonObject();
            Double slope = optionalDouble(point, "calibrationSlope");
            Double intercept = optionalDouble(point, "calibrationIntercept");
            points.add(new WearGlucosePoint(
                    Instant.parse(required(point, "timestamp").getAsString()),
                    required(point, "rawMgDl").getAsDouble(),
                    slope,
                    intercept));
        }
        return points;
    }

    private WearDisplayPolicy decodeDisplay(JsonObject json) {
        return new WearDisplayPolicy(
                GlucoseUnit.valueOf(required(json, "unit").getAsString()),
                required(json, "useCalibration").getAsBoolean(),
                required(json, "lowMgDl").getAsDouble(),
                required(json, "highMgDl").getAsDouble(),
                required(json, "graphWindowMinutes").getAsInt(),
                required(json, "graphZones").getAsBoolean(),
                required(json, "graphLines").getAsBoolean(),
                required(json, "trendArrow").getAsBoolean(),
                required(json, "trend").getAsString());
    }

    private WearAlertPolicy decodeAlerts(JsonObject json) {
        return new WearAlertPolicy(
                required(json, "lowEnabled").getAsBoolean(),
                required(json, "highEnabled").getAsBoolean(),
                required(json, "noDataEnabled").getAsBoolean(),
                Instant.parse(required(json, "snoozedUntil").getAsString()));
    }

    private WearAlertEvent decodeEvent(JsonObject json) {
        Instant measurementTimestamp = Instant.parse(
                required(json, "measurementTimestamp").getAsString());
        JsonElement generatedAt = json.get("generatedAt");
        return new WearAlertEvent(
                required(json, "eventId").getAsString(),
                AlertType.valueOf(required(json, "type").getAsString()),
                measurementTimestamp,
                generatedAt == null || generatedAt.isJsonNull()
                        ? measurementTimestamp
                        : Instant.parse(generatedAt.getAsString()),
                Instant.parse(required(json, "expiresAt").getAsString()));
    }

    private JsonElement required(JsonObject json, String name) {
        JsonElement value = json.get(name);
        if (value == null || value.isJsonNull()) {
            throw new WearProtocolException("Missing field: " + name);
        }
        return value;
    }

    private Double optionalDouble(JsonObject json, String name) {
        JsonElement value = json.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsDouble();
    }

    private void requireSize(byte[] payload) {
        if (payload == null || payload.length == 0) {
            throw new WearProtocolException("Snapshot payload is empty");
        }
        if (payload.length > WearSnapshot.MAX_PAYLOAD_BYTES) {
            throw new WearProtocolException("Snapshot payload is too large");
        }
    }
}
