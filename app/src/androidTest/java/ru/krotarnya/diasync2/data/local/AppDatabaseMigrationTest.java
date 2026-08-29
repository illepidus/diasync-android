package ru.krotarnya.diasync2.data.local;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseMigrationTest {
    private static final String TEST_DATABASE = "migration-test";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class);

    @Test
    public void migrate1To2PreservesPointsAndCursor() throws IOException {
        SupportSQLiteDatabase versionOne = helper.createDatabase(TEST_DATABASE, 1);
        versionOne.execSQL("INSERT INTO data_points ("
                + "user_id, timestamp, timestamp_epoch_second, timestamp_nano, sensor_mg_dl"
                + ") VALUES ('user-a', '2026-08-29T12:00:00Z', 1788004800, 0, 100.0)");
        versionOne.execSQL("INSERT INTO sync_state ("
                + "user_id, cursor_update_timestamp, last_success_at, last_error"
                + ") VALUES ('user-a', '2026-08-29T12:00:01Z', "
                + "'2026-08-29T12:00:02Z', NULL)");
        versionOne.close();

        SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DATABASE,
                2,
                true,
                AppDatabase.MIGRATION_1_2);

        try (android.database.Cursor cursor = migrated.query(
                "SELECT COUNT(*), source_fingerprint FROM sync_state "
                        + "WHERE user_id = 'user-a'")) {
            org.junit.Assert.assertTrue(cursor.moveToFirst());
            org.junit.Assert.assertEquals(1, cursor.getInt(0));
            org.junit.Assert.assertTrue(cursor.isNull(1));
        }
        try (android.database.Cursor cursor = migrated.query(
                "SELECT COUNT(*) FROM data_points WHERE user_id = 'user-a'")) {
            org.junit.Assert.assertTrue(cursor.moveToFirst());
            org.junit.Assert.assertEquals(1, cursor.getInt(0));
        }
    }
}
