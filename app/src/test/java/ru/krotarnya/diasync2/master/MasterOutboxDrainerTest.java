package ru.krotarnya.diasync2.master;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.master.XdripEvent;
import ru.krotarnya.diasync2.common.master.XdripEventCodec;
import ru.krotarnya.diasync2.common.master.XdripEventType;
import ru.krotarnya.diasync2.data.api.ApiDataPointDto;
import ru.krotarnya.diasync2.data.api.MasterUploadDataSource;
import ru.krotarnya.diasync2.data.api.MasterUploadHttpException;
import ru.krotarnya.diasync2.data.api.MasterUploadParseException;
import ru.krotarnya.diasync2.data.local.AppDatabase;
import ru.krotarnya.diasync2.data.local.DataPointEntity;
import ru.krotarnya.diasync2.data.local.MasterEventDao;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AppMode;
import ru.krotarnya.diasync2.settings.GraphWindow;

@RunWith(RobolectricTestRunner.class)
public class MasterOutboxDrainerTest {
    private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");
    private static final String USER_ID = "master-secret";

    private AppDatabase database;
    private MasterEventDao dao;
    private MasterEventRepository repository;
    private XdripEventCodec codec;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.masterEventDao();
        codec = new XdripEventCodec();
        repository = new MasterEventRepository(
                dao,
                Clock.fixed(NOW.minusSeconds(1), ZoneOffset.UTC));
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void successfulAckReconcilesMetadataAndMarksDelivered() {
        XdripEvent event = manual("MANUAL_GLUCOSE:a", 0L, 121.0);
        accept(event);
        MasterOutboxDrainer drainer = drainer(acknowledging());

        MasterOutboxDrainer.Result result = drainer.drainOnce(configuration());

        assertEquals(MasterOutboxDrainer.Kind.DELIVERED, result.kind());
        assertEquals(1, dao.countEventsInState(MasterEventState.DELIVERED.name()));
        DataPointEntity point = dao.findPoint(USER_ID, NOW.toString());
        assertEquals(Long.valueOf(100), point.serverId);
        assertEquals(NOW.plusSeconds(5).toString(), point.updateTimestamp);
        assertNotNull(dao.findEvent(event.eventId()).deliveredAt);
    }

    @Test
    public void uploadStateStartsOnlyWhenAnEligibleBatchExists() {
        AtomicBoolean uploadStarted = new AtomicBoolean();
        MasterOutboxDrainer drainer = drainer(acknowledging());

        drainer.drainOnce(configuration(), () -> uploadStarted.set(true));
        assertFalse(uploadStarted.get());

        accept(manual("MANUAL_GLUCOSE:a", 0L, 121.0));
        drainer.drainOnce(configuration(), () -> uploadStarted.set(true));
        assertTrue(uploadStarted.get());
    }

    @Test
    public void lostResponseRetriesAndBackendIdempotentAckCompletesEvent() {
        accept(manual("MANUAL_GLUCOSE:a", 0L, 121.0));
        AtomicInteger calls = new AtomicInteger();
        MasterUploadDataSource source = new FakeSource() {
            @Override
            public List<ApiDataPointDto> addDataPoints(
                    String baseUrl,
                    List<ApiDataPointDto> points
            ) throws IOException {
                if (calls.getAndIncrement() == 0) {
                    throw new IOException("response lost after server commit");
                }
                return acknowledge(points);
            }
        };
        MasterOutboxDrainer drainer = drainer(source);

        assertEquals(
                MasterOutboxDrainer.Kind.RETRY_SCHEDULED,
                drainer.drainOnce(configuration()).kind());
        assertEquals(MasterEventState.PENDING.name(),
                dao.findEvent("MANUAL_GLUCOSE:a").state);
        assertEquals(
                MasterOutboxDrainer.Kind.DELIVERED,
                drainer.drainOnce(configuration()).kind());
        assertEquals(2, calls.get());
    }

    @Test
    public void payloadTooLargeSplitsBatchUntilAccepted() {
        accept(manual("MANUAL_GLUCOSE:a", 0L, 121.0));
        accept(manual("MANUAL_GLUCOSE:b", 1_000L, 122.0));
        accept(manual("MANUAL_GLUCOSE:c", 2_000L, 123.0));
        AtomicInteger calls = new AtomicInteger();
        MasterUploadDataSource source = new FakeSource() {
            @Override
            public List<ApiDataPointDto> addDataPoints(
                    String baseUrl,
                    List<ApiDataPointDto> points
            ) throws MasterUploadHttpException {
                calls.incrementAndGet();
                if (points.size() > 1) {
                    throw new MasterUploadHttpException(413);
                }
                return acknowledge(points);
            }
        };

        MasterOutboxDrainer.Result result = drainer(source).drainOnce(configuration());

        assertEquals(MasterOutboxDrainer.Kind.DELIVERED, result.kind());
        assertEquals(3, dao.countEventsInState(MasterEventState.DELIVERED.name()));
        assertEquals(5, calls.get());
    }

    @Test
    public void permanent4xxBlocksWhile5xxSchedulesRetry() {
        accept(manual("MANUAL_GLUCOSE:a", 0L, 121.0));
        MasterOutboxDrainer blocked = drainer(failing(400));
        assertEquals(MasterOutboxDrainer.Kind.BLOCKED,
                blocked.drainOnce(configuration()).kind());
        assertEquals("HTTP_REJECTED", dao.findEvent("MANUAL_GLUCOSE:a").lastErrorCode);
        assertNull(dao.findEvent("MANUAL_GLUCOSE:a").leaseUntil);
        assertEquals(1, blocked.retryBlocked(configuration()));
        assertEquals(MasterEventState.PENDING.name(),
                dao.findEvent("MANUAL_GLUCOSE:a").state);

        accept(manual("MANUAL_GLUCOSE:b", 1_000L, 122.0));
        MasterOutboxDrainer retrying = drainer(failing(503));
        assertEquals(MasterOutboxDrainer.Kind.RETRY_SCHEDULED,
                retrying.drainOnce(configuration()).kind());
        assertEquals(MasterEventState.PENDING.name(),
                dao.findEvent("MANUAL_GLUCOSE:b").state);
    }

    @Test
    public void invalidAckNeverMarksEventDelivered() {
        accept(manual("MANUAL_GLUCOSE:a", 0L, 121.0));
        MasterUploadDataSource source = new FakeSource() {
            @Override
            public List<ApiDataPointDto> addDataPoints(
                    String baseUrl,
                    List<ApiDataPointDto> points
            ) {
                List<ApiDataPointDto> response = acknowledge(points);
                response.get(0).manualGlucose.mgdl = 999.0;
                return response;
            }
        };

        assertEquals(MasterOutboxDrainer.Kind.RETRY_SCHEDULED,
                drainer(source).drainOnce(configuration()).kind());
        assertEquals(0, dao.countEventsInState(MasterEventState.DELIVERED.name()));
    }

    @Test
    public void drainsLargeQueueInBoundedFifoBatches() {
        for (int index = 0; index < MasterOutboxDrainer.MAX_BATCH_SIZE + 1; index++) {
            accept(manual("MANUAL_GLUCOSE:" + index, index * 1_000L, 100.0 + index));
        }
        List<Integer> batchSizes = new ArrayList<>();
        MasterUploadDataSource source = new FakeSource() {
            @Override
            public List<ApiDataPointDto> addDataPoints(
                    String baseUrl,
                    List<ApiDataPointDto> points
            ) {
                batchSizes.add(points.size());
                return acknowledge(points);
            }
        };
        MasterOutboxDrainer drainer = drainer(source);

        assertEquals(MasterOutboxDrainer.Kind.DELIVERED,
                drainer.drainOnce(configuration()).kind());
        assertEquals(MasterOutboxDrainer.Kind.DELIVERED,
                drainer.drainOnce(configuration()).kind());

        assertEquals(List.of(MasterOutboxDrainer.MAX_BATCH_SIZE, 1), batchSizes);
        assertEquals(MasterOutboxDrainer.MAX_BATCH_SIZE + 1,
                dao.countEventsInState(MasterEventState.DELIVERED.name()));
    }

    @Test
    public void activeLeasePreventsConcurrentClaimAndExpiredLeaseIsRecovered() {
        accept(manual("MANUAL_GLUCOSE:a", 0L, 121.0));
        String fingerprint = MasterEventRepository.destinationFingerprint(configuration());
        String firstLease = NOW.plusSeconds(120).toString();

        assertEquals(1, dao.claimEligible(
                fingerprint,
                NOW.toString(),
                firstLease,
                10).size());
        assertEquals(0, dao.claimEligible(
                fingerprint,
                NOW.plusSeconds(30).toString(),
                NOW.plusSeconds(150).toString(),
                10).size());
        assertEquals(1, dao.claimEligible(
                fingerprint,
                NOW.plusSeconds(121).toString(),
                NOW.plusSeconds(241).toString(),
                10).size());
        assertEquals(2, dao.findEvent("MANUAL_GLUCOSE:a").attemptCount);
    }

    @Test
    public void acknowledgementBatchRollsBackIfAnyLeaseIsLost() {
        accept(manual("MANUAL_GLUCOSE:a", 0L, 121.0));
        accept(manual("MANUAL_GLUCOSE:b", 1_000L, 122.0));
        String fingerprint = MasterEventRepository.destinationFingerprint(configuration());
        String lease = NOW.plusSeconds(120).toString();
        dao.claimEligible(fingerprint, NOW.toString(), lease, 10);
        dao.markRetry(
                List.of("MANUAL_GLUCOSE:b"),
                lease,
                NOW.plusSeconds(1).toString(),
                "TEST_LEASE_LOST");
        List<MasterEventDao.DeliveryAck> acknowledgements = List.of(
                new MasterEventDao.DeliveryAck(
                        "MANUAL_GLUCOSE:a", NOW.toString(), 100L, NOW.plusSeconds(5).toString()),
                new MasterEventDao.DeliveryAck(
                        "MANUAL_GLUCOSE:b", NOW.plusSeconds(1).toString(), 101L,
                        NOW.plusSeconds(5).toString()));

        assertThrows(IllegalStateException.class, () -> dao.reconcileDeliveredBatch(
                acknowledgements,
                lease,
                NOW.plusSeconds(5).toString(),
                USER_ID));

        assertEquals(MasterEventState.IN_FLIGHT.name(),
                dao.findEvent("MANUAL_GLUCOSE:a").state);
        assertNull(dao.findPoint(USER_ID, NOW.toString()).serverId);
    }

    private void accept(XdripEvent event) {
        repository.accept(event, codec.encode(event), configuration());
    }

    private MasterOutboxDrainer drainer(MasterUploadDataSource source) {
        return new MasterOutboxDrainer(
                dao,
                source,
                codec,
                Clock.fixed(NOW, ZoneOffset.UTC),
                ignored -> Duration.ZERO);
    }

    private static MasterUploadDataSource acknowledging() {
        return new FakeSource() {
            @Override
            public List<ApiDataPointDto> addDataPoints(
                    String baseUrl,
                    List<ApiDataPointDto> points
            ) {
                return acknowledge(points);
            }
        };
    }

    private static MasterUploadDataSource failing(int code) {
        return new FakeSource() {
            @Override
            public List<ApiDataPointDto> addDataPoints(
                    String baseUrl,
                    List<ApiDataPointDto> points
            ) throws MasterUploadHttpException {
                throw new MasterUploadHttpException(code);
            }
        };
    }

    private static List<ApiDataPointDto> acknowledge(List<ApiDataPointDto> points) {
        long id = 100L;
        for (ApiDataPointDto point : points) {
            point.id = id++;
            point.updateTimestamp = NOW.plusSeconds(5).toString();
        }
        return points;
    }

    private static XdripEvent manual(String eventId, long offsetMillis, double value) {
        return new XdripEvent(
                1,
                eventId,
                XdripEventType.MANUAL_GLUCOSE,
                NOW.plusMillis(offsetMillis).toEpochMilli(),
                null,
                new XdripEvent.ManualGlucose(value),
                null);
    }

    private static AppConfiguration configuration() {
        return new AppConfiguration(
                AppMode.MASTER,
                "https://example.test/",
                USER_ID,
                GlucoseUnit.MMOL_L,
                true,
                70.0,
                180.0,
                GraphWindow.THIRTY_MINUTES,
                true,
                false,
                true);
    }

    private abstract static class FakeSource implements MasterUploadDataSource {
        @Override
        public abstract List<ApiDataPointDto> addDataPoints(
                String baseUrl,
                List<ApiDataPointDto> points
        ) throws IOException, MasterUploadHttpException, MasterUploadParseException;

        @Override
        public void cancelActiveCall() {
        }
    }
}
