package ru.krotarnya.diasync2.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import java.io.IOException;
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
import ru.krotarnya.diasync2.data.api.BootstrapDataSource;
import ru.krotarnya.diasync2.data.api.BootstrapHttpException;
import ru.krotarnya.diasync2.data.api.BootstrapParseException;
import ru.krotarnya.diasync2.data.local.AppDatabase;

@RunWith(RobolectricTestRunner.class)
public class BootstrapRepositoryTest {
    private static final String USER_ID = "bootstrap-secret";
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

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
    public void successfulBootstrapUsesExplicitWindowAndPersistsLatestSensorPoint() {
        CapturingDataSource source = new CapturingDataSource(List.of(sensorDto()));
        BootstrapRepository repository = repository(source);

        BootstrapResult result = repository.bootstrap("https://example.test", USER_ID);

        assertEquals(BootstrapResult.Kind.SUCCESS, result.kind());
        assertEquals(NOW.minus(BootstrapRepository.BOOTSTRAP_WINDOW), source.from);
        assertEquals(NOW, source.to);
        assertEquals(USER_ID, source.userId);
        assertEquals(1, database.bootstrapDao().count(USER_ID));
        assertEquals(100.0, result.latestPoint().sensorPoint().rawValue().mgDl(), 0.0001);
        assertNotNull(database.bootstrapDao().syncState(USER_ID).lastSuccessAt);
        assertNull(database.bootstrapDao().syncState(USER_ID).cursorUpdateTimestamp);
    }

    @Test
    public void emptyBootstrapIsNoDataAndStillRecordsSuccess() {
        BootstrapResult result = repository(new CapturingDataSource(List.of()))
                .bootstrap("https://example.test", USER_ID);

        assertEquals(BootstrapResult.Kind.NO_DATA, result.kind());
        assertEquals(0, database.bootstrapDao().count(USER_ID));
        assertNotNull(database.bootstrapDao().syncState(USER_ID).lastSuccessAt);
    }

    @Test
    public void errorsAreSafeStatesAndDoNotReplaceLocalData() {
        BootstrapRepository successful = repository(new CapturingDataSource(List.of(sensorDto())));
        successful.bootstrap("https://example.test", USER_ID);

        BootstrapResult connection = repository(new ThrowingDataSource(new IOException()))
                .bootstrap("https://example.test", USER_ID);
        BootstrapResult http = repository(new ThrowingDataSource(new BootstrapHttpException(500)))
                .bootstrap("https://example.test", USER_ID);
        BootstrapResult parse = repository(
                new ThrowingDataSource(new BootstrapParseException(new IllegalStateException())))
                .bootstrap("https://example.test", USER_ID);

        assertEquals(BootstrapResult.Kind.CONNECTION_ERROR, connection.kind());
        assertEquals(BootstrapResult.Kind.HTTP_ERROR, http.kind());
        assertEquals(BootstrapResult.Kind.PARSE_ERROR, parse.kind());
        assertEquals(1, database.bootstrapDao().count(USER_ID));
    }

    @Test
    public void mismatchedUserIsRejectedBeforeTransaction() {
        ApiDataPointDto dto = sensorDto();
        dto.userId = "another-user";

        BootstrapResult result = repository(new CapturingDataSource(List.of(dto)))
                .bootstrap("https://example.test", USER_ID);

        assertEquals(BootstrapResult.Kind.INVALID_DATA, result.kind());
        assertEquals(0, database.bootstrapDao().count(USER_ID));
        assertNull(database.bootstrapDao().syncState(USER_ID));
    }

    private BootstrapRepository repository(BootstrapDataSource source) {
        return new BootstrapRepository(
                source,
                database.bootstrapDao(),
                new DataPointMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ApiDataPointDto sensorDto() {
        ApiDataPointDto dto = new ApiDataPointDto();
        dto.id = 1L;
        dto.userId = USER_ID;
        dto.timestamp = "2026-08-27T11:59:00Z";
        dto.updateTimestamp = "2026-08-27T11:59:01Z";
        dto.sensorGlucose = new ApiDataPointDto.SensorGlucoseDto();
        dto.sensorGlucose.mgdl = 100.0;
        dto.sensorGlucose.sensorId = "sensor";
        return dto;
    }

    private static final class CapturingDataSource implements BootstrapDataSource {
        private final List<ApiDataPointDto> response;
        private String userId;
        private Instant from;
        private Instant to;

        private CapturingDataSource(List<ApiDataPointDto> response) {
            this.response = response;
        }

        @Override
        public List<ApiDataPointDto> getDataPoints(
                String baseUrl,
                String userId,
                Instant from,
                Instant to
        ) {
            this.userId = userId;
            this.from = from;
            this.to = to;
            return response;
        }
    }

    private static final class ThrowingDataSource implements BootstrapDataSource {
        private final Exception exception;

        private ThrowingDataSource(Exception exception) {
            this.exception = exception;
        }

        @Override
        public List<ApiDataPointDto> getDataPoints(
                String baseUrl,
                String userId,
                Instant from,
                Instant to
        ) throws IOException, BootstrapHttpException, BootstrapParseException {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            if (exception instanceof BootstrapHttpException httpException) {
                throw httpException;
            }
            if (exception instanceof BootstrapParseException parseException) {
                throw parseException;
            }
            throw new AssertionError(exception);
        }
    }
}
