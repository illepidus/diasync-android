package ru.krotarnya.diasync2.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BootstrapDaoTest {
    private static final String USER_ID = "room-secret";
    private static final String TIMESTAMP = "2026-08-27T10:00:00Z";

    private Context context;
    private AppDatabase database;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void repeatedBootstrapIsIdempotentAndChangedPointUpdatesRow() {
        BootstrapDao dao = database.bootstrapDao();
        SyncStateEntity state = new SyncStateEntity(
                USER_ID,
                null,
                "2026-08-27T10:01:00Z",
                null);

        dao.applyBootstrap(List.of(point(100.0, 1L)), state);
        dao.applyBootstrap(List.of(point(100.0, 1L)), state);

        assertEquals(1, dao.count(USER_ID));

        dao.applyBootstrap(List.of(point(125.0, 2L)), state);

        assertEquals(1, dao.count(USER_ID));
        assertEquals(Double.valueOf(125.0), dao.find(USER_ID, TIMESTAMP).sensorMgDl);
        assertEquals(Long.valueOf(2L), dao.find(USER_ID, TIMESTAMP).serverId);
        assertNotNull(dao.syncState(USER_ID));
    }

    @Test
    public void fileDatabaseKeepsPointsAfterReopen() {
        database.close();
        String databaseName = "room-reopen-test.db";
        context.deleteDatabase(databaseName);
        database = Room.databaseBuilder(context, AppDatabase.class, databaseName)
                .allowMainThreadQueries()
                .build();
        database.bootstrapDao().applyBootstrap(
                List.of(point(100.0, 1L)),
                new SyncStateEntity(USER_ID, null, "2026-08-27T10:01:00Z", null));
        database.close();

        database = Room.databaseBuilder(context, AppDatabase.class, databaseName)
                .allowMainThreadQueries()
                .build();

        assertEquals(1, database.bootstrapDao().count(USER_ID));
        assertNotNull(database.bootstrapDao().latestSensorPoint(USER_ID));
        context.deleteDatabase(databaseName);
    }

    private DataPointEntity point(double sensorMgDl, long serverId) {
        return new DataPointEntity(
                USER_ID,
                TIMESTAMP,
                1787824800L,
                0,
                serverId,
                "2026-08-27T10:00:01Z",
                sensorMgDl,
                "sensor",
                null,
                null,
                110.0,
                12.0,
                "meal");
    }
}
