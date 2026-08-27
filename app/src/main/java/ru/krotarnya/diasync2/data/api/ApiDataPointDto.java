package ru.krotarnya.diasync2.data.api;

public final class ApiDataPointDto {
    public Long id;
    public String userId;
    public String timestamp;
    public String updateTimestamp;
    public SensorGlucoseDto sensorGlucose;
    public ManualGlucoseDto manualGlucose;
    public CarbsDto carbs;

    public static final class SensorGlucoseDto {
        public Double mgdl;
        public String sensorId;
        public CalibrationDto calibration;
    }

    public static final class CalibrationDto {
        public Double slope;
        public Double intercept;
    }

    public static final class ManualGlucoseDto {
        public Double mgdl;
    }

    public static final class CarbsDto {
        public Double grams;
        public String description;
    }
}
