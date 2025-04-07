package ru.krotarnya.diasync.app.repository;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import ru.krotarnya.diasync.app.model.DataPoint;
import ru.krotarnya.diasync.app.model.DateConverters;

@Database(entities = {DataPoint.class}, version = 3)
@TypeConverters(DateConverters.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DataPointDao dataPointDao();
}