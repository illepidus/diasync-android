package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ru.krotarnya.diasync2.common.AlertType;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearAlertEvent;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

public class WearAlertControllerTest {
    private static final Instant NOW = Instant.parse("2026-08-29T12:00:00Z");

    @Test
    public void persistedDedupePreventsVibrationAfterControllerRestart() {
        FakeStateStore store = new FakeStateStore();
        List<AlertType> vibrations = new ArrayList<>();
        WearSnapshot snapshot = snapshot(WearAlertEvent.create(
                AlertType.HIGH, NOW.minusSeconds(20), NOW.minusSeconds(2)));

        controller(store, vibrations).onSnapshot(snapshot);
        controller(store, vibrations).onSnapshot(snapshot);

        assertEquals(List.of(AlertType.HIGH), vibrations);
        assertEquals(2, store.writeCount);
        assertTrue(store.state.lastProcessedEventId().startsWith("HIGH:"));
    }

    private WearAlertController controller(FakeStateStore store, List<AlertType> vibrations) {
        return new WearAlertController(
                new WearAlertEvaluator(Clock.fixed(NOW, ZoneOffset.UTC)),
                store,
                vibrations::add,
                ignored -> { },
                () -> { });
    }

    private WearSnapshot snapshot(WearAlertEvent event) {
        return new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                NOW,
                List.of(new WearGlucosePoint(NOW.minusSeconds(20), 190.0, null, null)),
                new WearDisplayPolicy(
                        GlucoseUnit.MMOL_L, true, 70.0, 180.0, 30,
                        true, false, true, "↑"),
                new WearAlertPolicy(true, true, true, Instant.EPOCH),
                event);
    }

    private static final class FakeStateStore implements WearAlertStateStore {
        private WearAlertState state = WearAlertState.empty();
        private int writeCount;

        @Override
        public WearAlertState read() {
            return state;
        }

        @Override
        public boolean write(WearAlertState state) {
            this.state = state;
            writeCount++;
            return true;
        }
    }
}
