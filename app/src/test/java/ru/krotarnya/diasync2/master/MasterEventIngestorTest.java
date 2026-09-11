package ru.krotarnya.diasync2.master;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.master.XdripEventCodec;
import ru.krotarnya.diasync2.data.local.AppDatabase;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AppMode;
import ru.krotarnya.diasync2.settings.GraphWindow;

@RunWith(RobolectricTestRunner.class)
public class MasterEventIngestorTest {
    private AppDatabase database;
    private boolean enabled;
    private AppConfiguration configuration;
    private AtomicInteger coordinatorCalls;
    private AtomicInteger wakeups;
    private List<String> diagnostics;
    private MasterEventIngestor ingestor;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        enabled = true;
        configuration = configuration(AppMode.MASTER);
        coordinatorCalls = new AtomicInteger();
        wakeups = new AtomicInteger();
        diagnostics = new ArrayList<>();
        ingestor = new MasterEventIngestor(
                () -> enabled,
                () -> Optional.ofNullable(configuration),
                new XdripEventCodec(),
                new MasterEventRepository(database.masterEventDao(), Clock.systemUTC()),
                coordinatorCalls::incrementAndGet,
                wakeups::incrementAndGet,
                diagnostics::add);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void canonicalDuplicateTriggersCoordinatorOnlyAfterFirstCommit() {
        String compact = validCarbsJson();
        String formatted = compact.replace("{", "{ ").replace(",", ", ");

        assertEquals(MasterEventIngestor.Result.ACCEPTED, ingestor.ingest(compact));
        assertEquals(MasterEventIngestor.Result.DUPLICATE, ingestor.ingest(formatted));

        assertEquals(1, database.masterEventDao().countEvents());
        assertEquals(1, coordinatorCalls.get());
        assertEquals(1, wakeups.get());
    }

    @Test
    public void malformedWrongVersionAndMissingPayloadDoNotWrite() {
        assertEquals(MasterEventIngestor.Result.REJECTED, ingestor.ingest("not-json"));
        assertEquals(
                MasterEventIngestor.Result.REJECTED,
                ingestor.ingest(validCarbsJson().replace("\"protocolVersion\":1", "\"protocolVersion\":2")));
        assertEquals(MasterEventIngestor.Result.REJECTED, ingestor.ingest(null));

        assertEquals(0, database.masterEventDao().countEvents());
        assertEquals(List.of("INVALID_EVENT", "INVALID_EVENT", "INVALID_EVENT"), diagnostics);
    }

    @Test
    public void slaveModeStoppedMonitoringAndMissingConfigurationDoNotWrite() {
        configuration = configuration(AppMode.SLAVE);
        assertEquals(MasterEventIngestor.Result.REJECTED, ingestor.ingest(validCarbsJson()));
        configuration = configuration(AppMode.MASTER);
        enabled = false;
        assertEquals(MasterEventIngestor.Result.REJECTED, ingestor.ingest(validCarbsJson()));
        enabled = true;
        configuration = null;
        assertEquals(MasterEventIngestor.Result.REJECTED, ingestor.ingest(validCarbsJson()));

        assertEquals(0, database.masterEventDao().countEvents());
        assertEquals(0, coordinatorCalls.get());
    }

    private static String validCarbsJson() {
        return "{\"protocolVersion\":1,"
                + "\"eventId\":\"CARBS:ingestor-test\","
                + "\"eventType\":\"CARBS\","
                + "\"occurredAtEpochMillis\":1789027320000,"
                + "\"carbs\":{\"grams\":20.0}}";
    }

    private static AppConfiguration configuration(AppMode mode) {
        return new AppConfiguration(
                mode,
                "https://example.test/",
                "master-user",
                GlucoseUnit.MMOL_L,
                true,
                70.0,
                180.0,
                GraphWindow.THIRTY_MINUTES,
                true,
                false,
                true);
    }
}
