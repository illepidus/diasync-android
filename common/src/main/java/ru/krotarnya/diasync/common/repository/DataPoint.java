package ru.krotarnya.diasync.common.repository;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.Instant;

import lombok.Data;

@Data
@Entity(
        tableName = "data",
        indices = {@Index(value = {"userId", "timestamp"}, unique = true)}
)
public class DataPoint {
    @PrimaryKey
    public long id;

    public String userId;
    public Instant timestamp;

    @Nullable
    @Embedded(prefix = "sensor_")
    public SensorGlucose sensorGlucose;

    @Embedded(prefix = "manual_")
    public ManualGlucose manualGlucose;

    @Embedded(prefix = "carbs_")
    public Carbs carbs;

    @Data
    public static class SensorGlucose {
        public double mgdl;
        public String sensorId;

        @Embedded(prefix = "cal_")
        public Calibration calibration;

        public double getMgdl(boolean useCalibrations) {
            if (calibration == null || !useCalibrations) return mgdl;
            return calibration.slope + calibration.intercept * mgdl;
        }
    }

    @Data
    public static class ManualGlucose {
        public double mgdl;
    }

    @Data
    public static class Carbs {
        public double grams;
        public String description;
    }

    @Data
    public static class Calibration {
        public double slope;
        public double intercept;
    }
}
