package ru.krotarnya.diasync2.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.krotarnya.diasync2.data.api.ApiDataPointDto;
import ru.krotarnya.diasync2.data.api.LongPollCall;
import ru.krotarnya.diasync2.data.api.LongPollDataSource;
import ru.krotarnya.diasync2.data.local.AppDatabase;
import ru.krotarnya.diasync2.data.local.BootstrapDao;
import ru.krotarnya.diasync2.data.local.DataPointEntity;
import ru.krotarnya.diasync2.data.local.SyncStateEntity;

@RunWith(RobolectricTestRunner.class)
public class LongPollRepositoryTest {
    private static final String USER_ID = "long-poll-secret";
    private static final Instant NOW = Instant.parse("2026-08-27T12:05:00Z");

    private AppDatabase database;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void batchAndMaximumServerCursorAreAtomicAndRepeatIsIdempotent() {
        ApiDataPointDto newerMeasurementWithOlderUpdate = point(
                "2026-08-27T12:04:00Z", "2026-08-27T12:04:02Z", 100.0);
        ApiDataPointDto olderMeasurementWithNewerUpdate = point(
                "2026-08-27T12:03:00Z", "2026-08-27T12:04:05Z", 110.0);
        LongPollRepository repository = repository(List.of(
                newerMeasurementWithOlderUpdate,
                olderMeasurementWithNewerUpdate));

        LongPollResult first = repository.poll("https://example.test", USER_ID, Instant.EPOCH);
        LongPollResult repeated = repository.poll("https://example.test", USER_ID, first.cursor());

        assertEquals(LongPollResult.Kind.DATA, first.kind());
        assertEquals(Instant.parse("2026-08-27T12:04:05Z"), first.cursor());
        assertEquals(first.cursor(), repeated.cursor());
        assertEquals(2, database.bootstrapDao().count(USER_ID));
        assertEquals(
                "2026-08-27T12:04:05Z",
                database.bootstrapDao().syncState(USER_ID).cursorUpdateTimestamp);
        assertEquals(
                SyncSourceFingerprint.from("https://example.test"),
                database.bootstrapDao().syncState(USER_ID).sourceFingerprint);
    }

    @Test
    public void missingCursorRejectsWholeBatchAndDoesNotAdvanceExistingCursor() {
        database.bootstrapDao().upsertSyncState(new SyncStateEntity(
                USER_ID,
                "2026-08-27T12:00:00Z",
                NOW.minusSeconds(60).toString(),
                null));
        ApiDataPointDto missingCursor = point("2026-08-27T12:04:00Z", null, 100.0);

        LongPollResult result = repository(List.of(missingCursor))
                .poll("https://example.test", USER_ID, Instant.EPOCH);

        assertEquals(LongPollResult.Kind.INVALID_DATA, result.kind());
        assertEquals(0, database.bootstrapDao().count(USER_ID));
        assertEquals(
                "2026-08-27T12:00:00Z",
                database.bootstrapDao().syncState(USER_ID).cursorUpdateTimestamp);
    }

    @Test
    public void malformedServerCursorIsInvalidDataAndDoesNotAdvanceExistingCursor() {
        database.bootstrapDao().upsertSyncState(new SyncStateEntity(
                USER_ID,
                "2026-08-27T12:00:00Z",
                NOW.minusSeconds(60).toString(),
                null));
        ApiDataPointDto malformed = point(
                "2026-08-27T12:04:00Z",
                "not-an-instant",
                100.0);

        LongPollResult result = repository(List.of(malformed))
                .poll("https://example.test", USER_ID, Instant.EPOCH);

        assertEquals(LongPollResult.Kind.INVALID_DATA, result.kind());
        assertEquals(0, database.bootstrapDao().count(USER_ID));
        assertEquals(
                "2026-08-27T12:00:00Z",
                database.bootstrapDao().syncState(USER_ID).cursorUpdateTimestamp);
    }

    @Test
    public void emptyTimeoutPreservesCursorAndIsNotAnError() {
        database.bootstrapDao().upsertSyncState(new SyncStateEntity(
                USER_ID,
                "2026-08-27T12:00:00Z",
                NOW.minusSeconds(60).toString(),
                "old error"));

        LongPollResult result = repository(List.of())
                .poll("https://example.test", USER_ID, Instant.EPOCH);

        assertEquals(LongPollResult.Kind.EMPTY, result.kind());
        SyncStateEntity state = database.bootstrapDao().syncState(USER_ID);
        assertEquals("2026-08-27T12:00:00Z", state.cursorUpdateTimestamp);
        assertEquals(NOW.toString(), state.lastSuccessAt);
        assertNull(state.lastError);
    }

    @Test
    public void crashBoundaryWithPointButNoCursorRecoversByRepeatingBatch() {
        ApiDataPointDto dto = point(
                "2026-08-27T12:04:00Z", "2026-08-27T12:04:05Z", 100.0);
        database.bootstrapDao().upsertDataPoints(List.of(
                new DataPointMapper().toEntity(dto, USER_ID)));

        assertNull(database.bootstrapDao().syncState(USER_ID));

        LongPollResult result = repository(List.of(dto))
                .poll("https://example.test", USER_ID, Instant.EPOCH);

        assertEquals(LongPollResult.Kind.DATA, result.kind());
        assertEquals(1, database.bootstrapDao().count(USER_ID));
        assertEquals(
                "2026-08-27T12:04:05Z",
                database.bootstrapDao().syncState(USER_ID).cursorUpdateTimestamp);
    }

    @Test
    public void commitFailureCannotAdvanceCursorPastPoints() {
        FailingCursorDao dao = new FailingCursorDao();
        LongPollRepository repository = new LongPollRepository(
                (baseUrl, userId, since) -> new ImmediateCall(List.of(point(
                        "2026-08-27T12:04:00Z",
                        "2026-08-27T12:04:05Z",
                        100.0))),
                dao,
                new DataPointMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        LongPollResult result = repository.poll(
                "https://example.test", USER_ID, Instant.EPOCH);

        assertEquals(LongPollResult.Kind.STORAGE_ERROR, result.kind());
        assertEquals(1, dao.pointCount);
        assertEquals("2026-08-27T12:00:00Z", dao.state.cursorUpdateTimestamp);
    }

    @Test
    public void cancelDelegatesToActiveHttpCall() throws Exception {
        BlockingCall blockingCall = new BlockingCall();
        LongPollRepository repository = new LongPollRepository(
                (baseUrl, userId, since) -> blockingCall,
                database.bootstrapDao(),
                new DataPointMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        Thread thread = new Thread(() -> repository.poll(
                "https://example.test", USER_ID, Instant.EPOCH));
        thread.start();
        blockingCall.awaitStarted();

        repository.cancelActiveCall();
        thread.join(1_000L);

        assertTrue(blockingCall.cancelled);
        assertFalse(thread.isAlive());
    }

    private LongPollRepository repository(List<ApiDataPointDto> response) {
        LongPollDataSource source = (baseUrl, userId, since) -> new ImmediateCall(response);
        return new LongPollRepository(
                source,
                database.bootstrapDao(),
                new DataPointMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ApiDataPointDto point(String timestamp, String updateTimestamp, double mgDl) {
        ApiDataPointDto dto = new ApiDataPointDto();
        dto.id = 1L;
        dto.userId = USER_ID;
        dto.timestamp = timestamp;
        dto.updateTimestamp = updateTimestamp;
        dto.sensorGlucose = new ApiDataPointDto.SensorGlucoseDto();
        dto.sensorGlucose.mgdl = mgDl;
        dto.sensorGlucose.sensorId = "sensor";
        return dto;
    }

    private static final class ImmediateCall implements LongPollCall {
        private final List<ApiDataPointDto> response;

        private ImmediateCall(List<ApiDataPointDto> response) {
            this.response = response;
        }

        @Override
        public List<ApiDataPointDto> execute() {
            return response;
        }

        @Override
        public void cancel() { }
    }

    private static final class BlockingCall implements LongPollCall {
        private final Object lock = new Object();
        private boolean started;
        private boolean cancelled;

        @Override
        public List<ApiDataPointDto> execute() throws java.io.IOException {
            synchronized (lock) {
                started = true;
                lock.notifyAll();
                while (!cancelled) {
                    try {
                        lock.wait();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new java.io.IOException(exception);
                    }
                }
            }
            throw new java.io.IOException("cancelled");
        }

        @Override
        public void cancel() {
            synchronized (lock) {
                cancelled = true;
                lock.notifyAll();
            }
        }

        private void awaitStarted() throws InterruptedException {
            synchronized (lock) {
                while (!started) {
                    lock.wait();
                }
            }
        }
    }

    private static final class FailingCursorDao implements BootstrapDao {
        private int pointCount;
        private final SyncStateEntity state = new SyncStateEntity(
                USER_ID,
                "2026-08-27T12:00:00Z",
                null,
                null);

        @Override
        public void upsertDataPoints(List<DataPointEntity> points) {
            pointCount += points.size();
        }

        @Override
        public void upsertSyncState(SyncStateEntity state) {
            throw new IllegalStateException("simulated commit failure");
        }

        @Override
        public DataPointEntity latestSensorPoint(String userId) {
            return null;
        }

        @Override
        public List<DataPointEntity> latestSensorPoints(String userId, int limit) {
            return List.of();
        }

        @Override
        public DataPointEntity find(String userId, String timestamp) {
            return null;
        }

        @Override
        public int count(String userId) {
            return pointCount;
        }

        @Override
        public SyncStateEntity syncState(String userId) {
            return state;
        }

        @Override
        public void recordEmptyPollSuccess(String userId, String lastSuccessAt) {
        }
    }
}
