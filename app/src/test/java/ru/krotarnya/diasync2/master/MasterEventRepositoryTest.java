package ru.krotarnya.diasync2.master;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.master.XdripEvent;
import ru.krotarnya.diasync2.common.master.XdripEventCodec;
import ru.krotarnya.diasync2.common.master.XdripEventType;
import ru.krotarnya.diasync2.data.local.AppDatabase;
import ru.krotarnya.diasync2.data.local.DataPointEntity;
import ru.krotarnya.diasync2.data.local.MasterEventDao;
import ru.krotarnya.diasync2.data.local.MasterEventEntity;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AppMode;
import ru.krotarnya.diasync2.settings.GraphWindow;

@RunWith(RobolectricTestRunner.class)
public class MasterEventRepositoryTest {
    private static final String USER_ID = "master-secret";
    private static final long OCCURRED_AT = 1_789_027_200_000L;
    private static final Instant RECEIVED_AT = Instant.parse("2026-09-10T12:00:00Z");

    private AppDatabase database;
    private MasterEventDao dao;
    private MasterEventRepository repository;
    private final XdripEventCodec codec = new XdripEventCodec();

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.masterEventDao();
        repository = new MasterEventRepository(
                dao,
                Clock.fixed(RECEIVED_AT, ZoneOffset.UTC));
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void sensorManualAndCarbsAtSameTimestampMergeWithoutErasingFields() {
        accept(sensor("SENSOR:sensor-a", 123.0));
        accept(manual("MANUAL_GLUCOSE:manual-a", 121.0));
        accept(carbs("CARBS:carbs-a", 20.0, "Snack"));

        DataPointEntity point = dao.findPoint(USER_ID, occurredAt().toString());
        assertEquals(Double.valueOf(123.0), point.sensorMgDl);
        assertEquals("libre-sensor", point.sensorId);
        assertEquals(Double.valueOf(1.1), point.calibrationSlope);
        assertEquals(Double.valueOf(-2.0), point.calibrationIntercept);
        assertEquals(Double.valueOf(121.0), point.manualMgDl);
        assertEquals(Double.valueOf(20.0), point.carbsGrams);
        assertEquals("Snack", point.carbsDescription);
        assertEquals(3, dao.countEvents());
        assertEquals(3, dao.countEventsInState(MasterEventState.PENDING.name()));
    }

    @Test
    public void duplicateDoesNotCreateAnotherPendingEvent() {
        XdripEvent event = sensor("SENSOR:sensor-a", 123.0);

        assertEquals(MasterEventDao.Acceptance.ACCEPTED, accept(event));
        assertEquals(MasterEventDao.Acceptance.DUPLICATE, accept(event));

        assertEquals(1, dao.countEvents());
    }

    @Test
    public void hashConflictDoesNotMutatePreviouslyAcceptedPoint() {
        assertEquals(
                MasterEventDao.Acceptance.ACCEPTED,
                accept(sensor("SENSOR:sensor-a", 123.0)));

        assertEquals(
                MasterEventDao.Acceptance.HASH_CONFLICT,
                accept(sensor("SENSOR:sensor-a", 222.0)));

        assertEquals(1, dao.countEvents());
        assertEquals(
                Double.valueOf(123.0),
                dao.findPoint(USER_ID, occurredAt().toString()).sensorMgDl);
    }

    @Test
    public void eventInsertFailureRollsBackPointChange() {
        XdripEvent event = sensor("SENSOR:sensor-a", 123.0);
        accept(event);
        MasterEventEntity existingEvent = dao.findEvent(event.eventId());
        DataPointEntity changedPoint = new DataPointEntity(
                USER_ID,
                occurredAt().toString(),
                occurredAt().getEpochSecond(),
                occurredAt().getNano(),
                null,
                null,
                999.0,
                "other-sensor",
                1.0,
                0.0,
                null,
                null,
                null);

        assertThrows(
                RuntimeException.class,
                () -> dao.insertPointAndEvent(changedPoint, existingEvent));

        assertEquals(
                Double.valueOf(123.0),
                dao.findPoint(USER_ID, occurredAt().toString()).sensorMgDl);
        assertEquals(1, dao.countEvents());
    }

    @Test
    public void destinationFingerprintDoesNotContainCredentialOrUrl() {
        String fingerprint = MasterEventRepository.destinationFingerprint(configuration());

        assertEquals(64, fingerprint.length());
        assertFalse(fingerprint.contains(USER_ID));
        assertFalse(fingerprint.contains("example.test"));
    }

    @Test
    public void nullCarbsDescriptionDoesNotEraseExistingDescription() {
        accept(carbs("CARBS:carbs-a", 20.0, "Snack"));
        accept(carbs("CARBS:carbs-b", 25.0, null));

        DataPointEntity point = dao.findPoint(USER_ID, occurredAt().toString());
        assertEquals(Double.valueOf(25.0), point.carbsGrams);
        assertEquals("Snack", point.carbsDescription);
        assertNull(point.serverId);
    }

    private MasterEventDao.Acceptance accept(XdripEvent event) {
        return repository.accept(event, codec.encode(event), configuration());
    }

    private static XdripEvent sensor(String eventId, double rawValue) {
        return new XdripEvent(
                1,
                eventId,
                XdripEventType.SENSOR,
                OCCURRED_AT,
                new XdripEvent.Sensor(
                        rawValue,
                        "libre-sensor",
                        new XdripEvent.Calibration(1.1, -2.0)),
                null,
                null);
    }

    private static XdripEvent manual(String eventId, double mgdl) {
        return new XdripEvent(
                1,
                eventId,
                XdripEventType.MANUAL_GLUCOSE,
                OCCURRED_AT,
                null,
                new XdripEvent.ManualGlucose(mgdl),
                null);
    }

    private static XdripEvent carbs(String eventId, double grams, String description) {
        return new XdripEvent(
                1,
                eventId,
                XdripEventType.CARBS,
                OCCURRED_AT,
                null,
                null,
                new XdripEvent.Carbs(grams, description));
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

    private static Instant occurredAt() {
        return Instant.ofEpochMilli(OCCURRED_AT);
    }
}
