package ru.krotarnya.diasync.common.model;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DataPoint {
    @PrimaryKey
    public long id;

    public String userId;
    public String timestamp;

    @Embedded(prefix = "sensor_")
    public SensorGlucose sensorGlucose;

    @Embedded(prefix = "manual_")
    public ManualGlucose manualGlucose;

    @Embedded(prefix = "carbs_")
    public Carbs carbs;

    public static class SensorGlucose {
        public Double mgdl;
        public String sensorId;

        @Embedded(prefix = "cal_")
        public Calibration calibration;
    }

    public static class ManualGlucose {
        public Double mgdl;
    }

    public static class Carbs {
        public Double grams;
        public String description;
    }

    public static class Calibration {
        public Double slope;
        public Double intercept;
    }
}
