package ru.krotarnya.diasync.common.repository;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.krotarnya.diasync.common.model.GlucoseUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "settings")
public class Settings {
    private static final Settings DEFAULT = Settings.builder()
            .id(0)
            .lowThreshold(70)
            .highThreshold(180)
            .userId("krotarino")
            .watchFaceTimeWindow(Duration.ofMinutes(30))
            .unit(GlucoseUnit.MMOL)
            .useCalibrations(true)
            .build();

    @PrimaryKey
    public long id;

    public double lowThreshold;
    public double highThreshold;

    public String userId;

    public Duration watchFaceTimeWindow;
    public GlucoseUnit unit;
    public boolean useCalibrations;

    public static Settings getDefault() {
        return DEFAULT;
    }
}
