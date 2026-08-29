package ru.krotarnya.diasync2.data.local;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {DataPointEntity.class, SyncStateEntity.class},
        version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public static final String NAME = "diasync.db";
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE sync_state ADD COLUMN source_fingerprint TEXT");
        }
    };

    public abstract BootstrapDao bootstrapDao();
}
