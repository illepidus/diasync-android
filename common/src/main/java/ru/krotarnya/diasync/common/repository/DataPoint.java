package ru.krotarnya.diasync.common.repository;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.time.Instant;

import lombok.Data;
import ru.krotarnya.diasync.common.model.DateConverters;

@Data
@Entity(
        tableName = "data",
        indices = {@Index(value = {"userId", "timestamp"}, unique = true)}
)
@TypeConverters(DateConverters.class)
public class DataPoint {
    @PrimaryKey
    public long id;

    public String userId;
    public Instant timestamp;

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
