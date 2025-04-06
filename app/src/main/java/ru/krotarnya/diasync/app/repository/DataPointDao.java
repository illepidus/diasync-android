package ru.krotarnya.diasync.app.repository;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ru.krotarnya.diasync.app.model.DataPoint;

@Dao
public interface DataPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DataPoint> points);

    @Query("SELECT * FROM DataPoint ORDER BY timestamp DESC")
    List<DataPoint> getAll();
}