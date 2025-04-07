package ru.krotarnya.diasync.common.model;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.gson.Gson;

import java.time.Instant;

@Entity(indices = {@Index(value = {"userId", "timestamp"}, unique = true)})
@TypeConverters(DateConverters.class)
public class DataPoint {
    private static final Gson gson = new Gson();

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

    @NonNull
    @Override
    public String toString() {
        return gson.toJson(this);
    }
}
