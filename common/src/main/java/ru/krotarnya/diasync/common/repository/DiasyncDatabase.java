package ru.krotarnya.diasync.common.repository;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import ru.krotarnya.diasync.common.model.DateTimeConverters;

@Database(
        entities = {DataPoint.class, Settings.class},
        version = 10,
        exportSchema = false
)
@TypeConverters(DateTimeConverters.class)
public abstract class DiasyncDatabase extends RoomDatabase {
    private static volatile DiasyncDatabase INSTANCE;
    private static final String DATABASE_NAME = "diasync";

    public static DiasyncDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DiasyncDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    DiasyncDatabase.class,
                                    DATABASE_NAME)
                            .fallbackToDestructiveMigration(true)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract DataPointDao dataPointDao();

    public abstract SettingsDao settingsDao();
}
