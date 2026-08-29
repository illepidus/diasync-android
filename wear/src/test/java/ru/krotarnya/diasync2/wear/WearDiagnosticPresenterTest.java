package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

public final class WearDiagnosticPresenterTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");
    private final WearDiagnosticPresenter presenter = new WearDiagnosticPresenter(
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test public void rejectedPayloadIsExplicitWithoutSnapshot() {
        WearDiagnosticState state = presenter.present(Optional.empty(), null,
                "Unsupported or invalid snapshot", WearDataPhase.FRESH);
        assertEquals("REJECTED PAYLOAD", state.headline());
        assertEquals("Unsupported or invalid snapshot", state.error());
    }

    @Test public void ageMapsToValidStaleAndNoData() {
        assertEquals("VALID", stateAt(NOW.minusSeconds(89)).headline());
        assertEquals("STALE", stateAt(NOW.minusSeconds(90)).headline());
        assertEquals("NO DATA", stateAt(NOW.minusSeconds(300)).headline());
        assertTrue(stateAt(NOW.minusSeconds(30)).details().contains("Watchdog: FRESH"));
    }

    private WearDiagnosticState stateAt(Instant pointTime) {
        WearSnapshot snapshot = new WearSnapshot(WearSnapshot.PROTOCOL_VERSION, NOW,
                List.of(new WearGlucosePoint(pointTime, 126, null, null)),
                new WearDisplayPolicy(GlucoseUnit.MMOL_L, true, 70, 180, 30,
                        true, false, true, "→"),
                new WearAlertPolicy(false, false, true, Instant.EPOCH), null);
        return presenter.present(Optional.of(snapshot), NOW.minusSeconds(5), null, WearDataPhase.FRESH);
    }
}
