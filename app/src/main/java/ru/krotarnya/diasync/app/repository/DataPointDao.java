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

    /** @noinspection unused*/
    @Query("SELECT * FROM DataPoint " +
            "WHERE userId = :userId " +
            "AND timestamp >= :fromInclusive " +
            "AND timestamp < :toExclusive " +
            "ORDER BY timestamp DESC")
    List<DataPoint> getSince(String userId, String fromInclusive, String toExclusive);
}