package ru.krotarnya.diasync2.data;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.Objects;
import ru.krotarnya.diasync2.common.Calibration;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseValue;
import ru.krotarnya.diasync2.common.SensorPoint;
import ru.krotarnya.diasync2.data.api.ApiDataPointDto;
import ru.krotarnya.diasync2.data.local.DataPointEntity;

public final class DataPointMapper {
    public DataPointEntity toEntity(ApiDataPointDto dto, String expectedUserId) {
        Objects.requireNonNull(dto);
        if (!expectedUserId.equals(dto.userId)) {
            throw new InvalidDataException("Response user does not match configuration");
        }
        Instant timestamp = parseRequiredInstant(dto.timestamp);
        Instant updateTimestamp = parseOptionalInstant(dto.updateTimestamp);

        Double calibrationSlope = null;
        Double calibrationIntercept = null;
        if (dto.sensorGlucose != null && dto.sensorGlucose.calibration != null) {
            calibrationSlope = requireFinite(dto.sensorGlucose.calibration.slope, "calibration slope");
            calibrationIntercept = requireFinite(
                    dto.sensorGlucose.calibration.intercept,
                    "calibration intercept");
        }
        Double sensorMgDl = dto.sensorGlucose == null
                ? null
                : requireNonNegative(dto.sensorGlucose.mgdl, "sensor glucose");
        Double manualMgDl = dto.manualGlucose == null
                ? null
                : requireNonNegative(dto.manualGlucose.mgdl, "manual glucose");
        Double carbsGrams = dto.carbs == null
                ? null
                : requireNonNegative(dto.carbs.grams, "carbs");

        return new DataPointEntity(
                expectedUserId,
                timestamp.toString(),
                timestamp.getEpochSecond(),
                timestamp.getNano(),
                dto.id,
                updateTimestamp == null ? null : updateTimestamp.toString(),
                sensorMgDl,
                dto.sensorGlucose == null ? null : dto.sensorGlucose.sensorId,
                calibrationSlope,
                calibrationIntercept,
                manualMgDl,
                carbsGrams,
                dto.carbs == null ? null : dto.carbs.description);
    }

    public DataPoint toDomain(DataPointEntity entity) {
        Instant timestamp = Instant.parse(entity.timestamp);
        Calibration calibration = entity.calibrationSlope == null
                ? null
                : new Calibration(entity.calibrationSlope, entity.calibrationIntercept);
        SensorPoint sensorPoint = entity.sensorMgDl == null
                ? null
                : new SensorPoint(
                        timestamp,
                        new GlucoseValue(entity.sensorMgDl),
                        entity.sensorId,
                        calibration);
        return new DataPoint(
                timestamp,
                entity.updateTimestamp == null ? null : Instant.parse(entity.updateTimestamp),
                sensorPoint,
                entity.manualMgDl == null ? null : new GlucoseValue(entity.manualMgDl),
                entity.carbsGrams,
                entity.carbsDescription);
    }

    private static Instant parseRequiredInstant(String value) {
        if (value == null) {
            throw new InvalidDataException("Missing timestamp");
        }
        Instant instant = parseOptionalInstant(value);
        if (instant == null) {
            throw new InvalidDataException("Missing timestamp");
        }
        return instant;
    }

    private static Instant parseOptionalInstant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeException exception) {
            throw new InvalidDataException("Invalid timestamp", exception);
        }
    }

    private static Double requireNonNegative(Double value, String field) {
        Double checked = requireFinite(value, field);
        if (checked < 0.0) {
            throw new InvalidDataException("Invalid " + field);
        }
        return checked;
    }

    private static Double requireFinite(Double value, String field) {
        if (value == null || !Double.isFinite(value)) {
            throw new InvalidDataException("Invalid " + field);
        }
        return value;
    }

    public static final class InvalidDataException extends IllegalArgumentException {
        public InvalidDataException(String message) {
            super(message);
        }

        public InvalidDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
