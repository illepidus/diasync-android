package ru.krotarnya.diasync2.data.local;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {DataPointEntity.class, SyncStateEntity.class, MasterEventEntity.class},
        version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public static final String NAME = "diasync.db";
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE sync_state ADD COLUMN source_fingerprint TEXT");
        }
    };
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS master_events ("
                    + "event_id TEXT NOT NULL, "
                    + "payload_hash TEXT NOT NULL, "
                    + "event_type TEXT NOT NULL, "
                    + "occurred_at TEXT NOT NULL, "
                    + "destination_fingerprint TEXT NOT NULL, "
                    + "payload_json TEXT NOT NULL, "
                    + "state TEXT NOT NULL, "
                    + "attempt_count INTEGER NOT NULL, "
                    + "next_attempt_at TEXT, "
                    + "lease_until TEXT, "
                    + "received_at TEXT NOT NULL, "
                    + "delivered_at TEXT, "
                    + "last_error_code TEXT, "
                    + "PRIMARY KEY(event_id))");
            database.execSQL("CREATE INDEX IF NOT EXISTS "
                    + "index_master_events_state_next_attempt_at_received_at "
                    + "ON master_events (state, next_attempt_at, received_at)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_master_events_delivered_at "
                    + "ON master_events (delivered_at)");
        }
    };

    public abstract BootstrapDao bootstrapDao();

    public abstract MasterEventDao masterEventDao();
}
