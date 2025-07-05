package ru.krotarnya.diasync.common.repository;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import ru.krotarnya.diasync.common.model.DateTimeConverters;

@androidx.room.Database(
        entities = {DataPoint.class, Settings.class},
        version = 9,
        exportSchema = false
)

@TypeConverters(DateTimeConverters.class)
public abstract class Database extends RoomDatabase {
    public static Database cons(Context context) {
        return Room.databaseBuilder(context, Database.class, "diasync-db")
                .fallbackToDestructiveMigration(true)
                .allowMainThreadQueries()
                .build();
    }

    public abstract DataPointDao dataPointDao();

    public abstract SettingsDao settingsDao();
}