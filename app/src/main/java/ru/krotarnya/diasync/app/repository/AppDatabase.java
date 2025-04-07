package ru.krotarnya.diasync.app.repository;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import ru.krotarnya.diasync.app.model.DataPoint;

@Database(entities = {DataPoint.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DataPointDao dataPointDao();
}