package ru.krotarnya.diasync2.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(
        tableName = "data_points",
        primaryKeys = {"user_id", "timestamp"},
        indices = {
                @Index(value = {"user_id", "timestamp"}),
                @Index(value = {"user_id", "update_timestamp"})
        })
public final class DataPointEntity {
    @NonNull
    @ColumnInfo(name = "user_id")
    public final String userId;

    @NonNull
    public final String timestamp;

    @ColumnInfo(name = "timestamp_epoch_second")
    public final long timestampEpochSecond;

    @ColumnInfo(name = "timestamp_nano")
    public final int timestampNano;

    @ColumnInfo(name = "server_id")
    public final Long serverId;

    @ColumnInfo(name = "update_timestamp")
    public final String updateTimestamp;

    @ColumnInfo(name = "sensor_mg_dl")
    public final Double sensorMgDl;

    @ColumnInfo(name = "sensor_id")
    public final String sensorId;

    @ColumnInfo(name = "calibration_slope")
    public final Double calibrationSlope;

    @ColumnInfo(name = "calibration_intercept")
    public final Double calibrationIntercept;

    @ColumnInfo(name = "manual_mg_dl")
    public final Double manualMgDl;

    @ColumnInfo(name = "carbs_grams")
    public final Double carbsGrams;

    @ColumnInfo(name = "carbs_description")
    public final String carbsDescription;

    public DataPointEntity(
            @NonNull String userId,
            @NonNull String timestamp,
            long timestampEpochSecond,
            int timestampNano,
            Long serverId,
            String updateTimestamp,
            Double sensorMgDl,
            String sensorId,
            Double calibrationSlope,
            Double calibrationIntercept,
            Double manualMgDl,
            Double carbsGrams,
            String carbsDescription
    ) {
        this.userId = userId;
        this.timestamp = timestamp;
        this.timestampEpochSecond = timestampEpochSecond;
        this.timestampNano = timestampNano;
        this.serverId = serverId;
        this.updateTimestamp = updateTimestamp;
        this.sensorMgDl = sensorMgDl;
        this.sensorId = sensorId;
        this.calibrationSlope = calibrationSlope;
        this.calibrationIntercept = calibrationIntercept;
        this.manualMgDl = manualMgDl;
        this.carbsGrams = carbsGrams;
        this.carbsDescription = carbsDescription;
    }
}
