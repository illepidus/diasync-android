package ru.krotarnya.diasync2.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {DataPointEntity.class, SyncStateEntity.class},
        version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract BootstrapDao bootstrapDao();
}
