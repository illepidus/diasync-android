package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;
import ru.krotarnya.diasync2.common.wear.WearSnapshotCodec;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class LastKnownWearStateRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");
    private final WearSnapshotCodec codec = new WearSnapshotCodec();

    @Test
    public void validPayloadReplacesStateAndRepeatIsIdempotent() {
        FakeStore store = new FakeStore();
        LastKnownWearStateRepository repository = new LastKnownWearStateRepository(store, codec);
        byte[] payload = codec.encode(snapshot(120.0));

        assertTrue(repository.replaceIfValid(payload));
        assertTrue(repository.replaceIfValid(payload));

        assertEquals(1, store.writeCount);
        assertEquals(120.0, repository.load().orElseThrow().points().get(0).rawMgDl(), 0.0);
    }

    @Test
    public void corruptAndUnsupportedPayloadDoNotReplaceLastValidState() {
        FakeStore store = new FakeStore();
        LastKnownWearStateRepository repository = new LastKnownWearStateRepository(store, codec);
        byte[] valid = codec.encode(snapshot(120.0));
        repository.replaceIfValid(valid);
        byte[] unsupported = new String(valid, StandardCharsets.UTF_8)
                .replace("\"v\":1", "\"v\":2")
                .getBytes(StandardCharsets.UTF_8);

        assertFalse(repository.replaceIfValid("{".getBytes(StandardCharsets.UTF_8)));
        assertFalse(repository.replaceIfValid(unsupported));

        assertArrayEquals(valid, store.payload);
        assertEquals(1, store.writeCount);
    }

    @Test
    public void newRepositoryReadsDeviceProtectedPersistedState() {
        Application application = RuntimeEnvironment.getApplication();
        application.createDeviceProtectedStorageContext()
                .getSharedPreferences("wear_last_known_state", 0)
                .edit()
                .clear()
                .commit();
        LastKnownWearStateRepository first = LastKnownWearStateRepository.create(application);
        assertTrue(first.replaceIfValid(codec.encode(snapshot(140.0))));

        LastKnownWearStateRepository restarted = LastKnownWearStateRepository.create(application);

        assertEquals(140.0, restarted.load().orElseThrow().points().get(0).rawMgDl(), 0.0);
    }

    private WearSnapshot snapshot(double value) {
        return new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                NOW,
                List.of(new WearGlucosePoint(NOW.minusSeconds(60), value, null, null)),
                new WearDisplayPolicy(
                        GlucoseUnit.MMOL_L,
                        true,
                        70.0,
                        180.0,
                        30,
                        true,
                        false,
                        true,
                        ""),
                new WearAlertPolicy(false, false, true, Instant.EPOCH),
                null);
    }

    private static final class FakeStore implements WearSnapshotStore {
        private byte[] payload;
        private int writeCount;

        @Override
        public byte[] read() {
            return payload;
        }

        @Override
        public boolean write(byte[] payload) {
            this.payload = payload;
            writeCount++;
            return true;
        }
    }
}
