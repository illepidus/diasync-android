package ru.krotarnya.diasync.common.repository;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import ru.krotarnya.diasync.common.model.DataPoint;
import ru.krotarnya.diasync.common.model.DateConverters;

@Database(entities = {DataPoint.class}, version = 3)
@TypeConverters(DateConverters.class)
public abstract class DiasyncDatabase extends RoomDatabase {
    public static DiasyncDatabase cons(Context context) {
        return Room.databaseBuilder(context, DiasyncDatabase.class, "diasync-db")
                .fallbackToDestructiveMigration(true)
                .allowMainThreadQueries()
                .build();
    }

    public abstract DataPointDao dataPointDao();
}