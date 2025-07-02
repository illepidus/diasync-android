package ru.krotarnya.diasync.common.repository;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;

@Data
@Entity(tableName = "settings")
public class Settings {
    @PrimaryKey
    public long id = 1;

    public double lowThreshold = 70.0;
    public double highThreshold = 180.0;
}
