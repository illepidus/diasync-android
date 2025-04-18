package ru.krotarnya.diasync.common.repository;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;
import java.util.Optional;

import ru.krotarnya.diasync.common.model.DataPoint;

@Dao
public interface DataPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(List<DataPoint> points);

    @Query("SELECT * FROM DataPoint " +
            "WHERE userId = :userId " +
            "AND timestamp >= :fromInclusive " +
            "AND timestamp < :toExclusive " +
            "ORDER BY timestamp DESC")
    List<DataPoint> find(String userId, String fromInclusive, String toExclusive);

    @Query("SELECT * FROM DataPoint " +
            "WHERE userId = :userId " +
            "ORDER BY timestamp DESC " +
            "LIMIT 1")
    Optional<DataPoint> findLast(String userId);
}