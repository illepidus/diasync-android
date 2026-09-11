package ru.krotarnya.diasync2.common.master;

import java.util.Objects;

public record XdripEvent(
        int protocolVersion,
        String eventId,
        XdripEventType eventType,
        long occurredAtEpochMillis,
        Sensor sensor,
        ManualGlucose manualGlucose,
        Carbs carbs
) {
    public static final int PROTOCOL_VERSION = 1;
    public static final int MAX_PAYLOAD_BYTES = 8 * 1024;
    public static final int MAX_EVENT_ID_LENGTH = 128;
    public static final int MAX_SENSOR_ID_LENGTH = 128;
    public static final int MAX_DESCRIPTION_LENGTH = 256;
    public static final double MAX_SENSOR_RAW_VALUE = 1_000_000.0;
    public static final double MAX_GLUCOSE_MG_DL = 1_000.0;
    public static final double MAX_CARBS_GRAMS = 1_000.0;
    public static final double MAX_CALIBRATION_SLOPE = 1_000.0;
    public static final double MAX_ABSOLUTE_CALIBRATION_INTERCEPT = 1_000_000.0;

    public XdripEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        if (protocolVersion != PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Protocol version is unsupported");
        }
        if (occurredAtEpochMillis < 0) {
            throw new IllegalArgumentException("Event timestamp is invalid");
        }
        requireIdentifier(eventId, MAX_EVENT_ID_LENGTH, "eventId");
        if (!eventId.startsWith(eventType.name() + ":")
                || eventId.length() == eventType.name().length() + 1) {
            throw new IllegalArgumentException("Event id does not match event type");
        }

        int subtypeCount = (sensor == null ? 0 : 1)
                + (manualGlucose == null ? 0 : 1)
                + (carbs == null ? 0 : 1);
        if (subtypeCount != 1) {
            throw new IllegalArgumentException("Event must contain exactly one payload subtype");
        }
        boolean matchingSubtype = switch (eventType) {
            case SENSOR -> sensor != null;
            case MANUAL_GLUCOSE -> manualGlucose != null;
            case CARBS -> carbs != null;
        };
        if (!matchingSubtype) {
            throw new IllegalArgumentException("Payload subtype does not match event type");
        }
    }

    public record Sensor(
            double rawValue,
            String sensorId,
            Calibration calibration
    ) {
        public Sensor {
            requirePositiveFinite(rawValue, MAX_SENSOR_RAW_VALUE, "sensor raw value");
            requireIdentifier(sensorId, MAX_SENSOR_ID_LENGTH, "sensorId");
            Objects.requireNonNull(calibration, "calibration");
        }
    }

    public record Calibration(double slope, double intercept) {
        public Calibration {
            requirePositiveFinite(slope, MAX_CALIBRATION_SLOPE, "calibration slope");
            if (!Double.isFinite(intercept)
                    || Math.abs(intercept) > MAX_ABSOLUTE_CALIBRATION_INTERCEPT) {
                throw new IllegalArgumentException("Calibration intercept is invalid");
            }
        }

        public static Calibration identity() {
            return new Calibration(1.0, 0.0);
        }
    }

    public record ManualGlucose(double mgdl) {
        public ManualGlucose {
            requirePositiveFinite(mgdl, MAX_GLUCOSE_MG_DL, "manual glucose");
        }
    }

    public record Carbs(double grams, String description) {
        public Carbs {
            requirePositiveFinite(grams, MAX_CARBS_GRAMS, "carbs");
            if (description != null) {
                requireText(description, MAX_DESCRIPTION_LENGTH, "carbs description");
            }
        }
    }

    private static void requirePositiveFinite(double value, double maximum, String name) {
        if (!Double.isFinite(value) || value <= 0.0 || value > maximum) {
            throw new IllegalArgumentException(name + " is invalid");
        }
    }

    private static void requireIdentifier(String value, int maximumLength, String name) {
        requireText(value, maximumLength, name);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 0x21 || character > 0x7e) {
                throw new IllegalArgumentException(name + " contains unsupported characters");
            }
        }
    }

    private static void requireText(String value, int maximumLength, String name) {
        if (value.isBlank() || !value.equals(value.strip())
                || value.codePointCount(0, value.length()) > maximumLength) {
            throw new IllegalArgumentException(name + " is invalid");
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException(name + " contains control characters");
            }
        }
    }
}
