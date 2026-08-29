package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.Test;
import ru.krotarnya.diasync2.common.AlertType;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearAlertEvent;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

public class WearAlertEvaluatorTest {
    private static final Instant NOW = Instant.parse("2026-08-29T12:00:00Z");

    @Test
    public void freshEventIsHandledOnceAndExpiredEventIsOnlyDeduplicated() {
        WearAlertEvaluator evaluator = evaluatorAt(NOW);
        WearAlertEvent fresh = WearAlertEvent.create(
                AlertType.LOW, NOW.minusSeconds(30), NOW.minusSeconds(5));

        WearAlertEvaluation first = evaluator.evaluate(
                snapshot(NOW.minusSeconds(30), fresh, policy(true, true, true, Instant.EPOCH)),
                WearAlertState.empty());
        WearAlertEvaluation repeat = evaluator.evaluate(
                snapshot(NOW.minusSeconds(30), fresh, policy(true, true, true, Instant.EPOCH)),
                first.state());
        WearAlertEvent expired = WearAlertEvent.create(
                AlertType.HIGH, NOW.minusSeconds(180), NOW.minusSeconds(180));
        WearAlertEvaluation reconnected = evaluator.evaluate(
                snapshot(NOW.minusSeconds(180), expired, policy(true, true, true, Instant.EPOCH)),
                WearAlertState.empty());

        assertEquals(AlertType.LOW, first.vibration());
        assertEquals(fresh.eventId(), first.state().lastProcessedEventId());
        assertNull(repeat.vibration());
        assertNull(reconnected.vibration());
        assertEquals(expired.eventId(), reconnected.state().lastProcessedEventId());
    }

    @Test
    public void visualStaleAndNoDataUseIndependentNinetySecondAndFiveMinuteBoundaries() {
        WearAlertEvaluator evaluator = evaluatorAt(NOW);

        WearAlertEvaluation atNinetySeconds = evaluator.evaluate(
                snapshot(NOW.minusSeconds(90), null, policy(false, false, true, Instant.EPOCH)),
                WearAlertState.empty());
        WearAlertEvaluation stale = evaluator.evaluate(
                snapshot(NOW.minusSeconds(91), null, policy(false, false, true, Instant.EPOCH)),
                WearAlertState.empty());
        WearAlertEvaluation noData = evaluator.evaluate(
                snapshot(NOW.minusSeconds(300), null, policy(false, false, true, Instant.EPOCH)),
                WearAlertState.empty());

        assertEquals(WearDataPhase.FRESH, atNinetySeconds.state().dataPhase());
        assertEquals(WearDataPhase.STALE, stale.state().dataPhase());
        assertNull(stale.vibration());
        assertEquals(WearDataPhase.NO_DATA, noData.state().dataPhase());
        assertEquals(AlertType.NO_DATA, noData.vibration());
    }

    @Test
    public void emptySnapshotSchedulesAndRaisesLocalNoDataAfterFiveMinutes() {
        WearSnapshot empty = new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                NOW,
                List.of(),
                new WearDisplayPolicy(
                        GlucoseUnit.MMOL_L, true, 70.0, 180.0, 30,
                        true, false, true, ""),
                policy(false, false, true, Instant.EPOCH),
                null);

        WearAlertEvaluation received = evaluatorAt(NOW).evaluate(empty, WearAlertState.empty());
        WearAlertEvaluation afterDisconnect = evaluatorAt(NOW.plusSeconds(300)).evaluate(
                empty,
                received.state());

        assertEquals(NOW.plusSeconds(300), received.nextCheckAt());
        assertNull(received.vibration());
        assertEquals(AlertType.NO_DATA, afterDisconnect.vibration());
    }

    @Test
    public void snoozeSuppressesUntilBoundaryAndClockJumpDoesNotUseRealSleep() {
        Instant snoozedUntil = NOW.plusSeconds(30);
        WearSnapshot source = snapshot(
                NOW.minusSeconds(600), null, policy(false, false, true, snoozedUntil));

        WearAlertEvaluation suppressed = evaluatorAt(NOW).evaluate(source, WearAlertState.empty());
        WearAlertEvaluation resumed = evaluatorAt(snoozedUntil).evaluate(
                source, suppressed.state());
        WearAlertEvaluation jumpedBack = evaluatorAt(NOW.minusSeconds(120)).evaluate(
                source, resumed.state());

        assertNull(suppressed.vibration());
        assertEquals(snoozedUntil, suppressed.nextCheckAt());
        assertEquals(AlertType.NO_DATA, resumed.vibration());
        assertNull(jumpedBack.vibration());
        assertEquals(NOW.minusSeconds(60), jumpedBack.nextCheckAt());
    }

    @Test
    public void farFuturePointGetsBoundedClockAnomalyRecheck() {
        WearAlertEvaluation future = evaluatorAt(NOW).evaluate(
                snapshot(
                        NOW.plusSeconds(600),
                        null,
                        policy(false, false, true, Instant.EPOCH)),
                WearAlertState.empty());

        assertNull(future.vibration());
        assertEquals(WearDataPhase.FRESH, future.state().dataPhase());
        assertEquals(NOW.plusSeconds(60), future.nextCheckAt());
    }

    @Test
    public void snoozeAndDisabledPolicySuppressFreshGlucoseEvents() {
        WearAlertEvent low = WearAlertEvent.create(
                AlertType.LOW, NOW.minusSeconds(20), NOW.minusSeconds(2));
        WearAlertEvent high = WearAlertEvent.create(
                AlertType.HIGH, NOW.minusSeconds(20), NOW.minusSeconds(2));

        WearAlertEvaluation snoozed = evaluatorAt(NOW).evaluate(
                snapshot(
                        NOW.minusSeconds(20),
                        low,
                        policy(true, true, false, NOW.plusSeconds(30))),
                WearAlertState.empty());
        WearAlertEvaluation disabled = evaluatorAt(NOW).evaluate(
                snapshot(
                        NOW.minusSeconds(20),
                        high,
                        policy(true, false, false, Instant.EPOCH)),
                WearAlertState.empty());

        assertNull(snoozed.vibration());
        assertEquals(low.eventId(), snoozed.state().lastProcessedEventId());
        assertNull(disabled.vibration());
        assertEquals(high.eventId(), disabled.state().lastProcessedEventId());
    }

    @Test
    public void freshRecoveryClearsNoDataRepeatAndRequestsFaceChange() {
        WearAlertState noDataState = new WearAlertState(
                null, NOW.minusSeconds(60), WearDataPhase.NO_DATA);

        WearAlertEvaluation recovered = evaluatorAt(NOW).evaluate(
                snapshot(NOW.minusSeconds(10), null, policy(false, false, true, Instant.EPOCH)),
                noDataState);

        assertNull(recovered.state().lastNoDataAlertAt());
        assertEquals(WearDataPhase.FRESH, recovered.state().dataPhase());
        assertTrue(recovered.complicationChanged());
        assertFalse(recovered.nextCheckAt().isBefore(NOW.plusSeconds(80)));
    }

    private WearAlertEvaluator evaluatorAt(Instant now) {
        return new WearAlertEvaluator(Clock.fixed(now, ZoneOffset.UTC));
    }

    private WearSnapshot snapshot(
            Instant latestTimestamp,
            WearAlertEvent event,
            WearAlertPolicy policy
    ) {
        return new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                latestTimestamp.isAfter(NOW) ? latestTimestamp : NOW,
                List.of(new WearGlucosePoint(latestTimestamp, 110.0, null, null)),
                new WearDisplayPolicy(
                        GlucoseUnit.MMOL_L, true, 70.0, 180.0, 30,
                        true, false, true, ""),
                policy,
                event);
    }

    private WearAlertPolicy policy(
            boolean low,
            boolean high,
            boolean noData,
            Instant snoozedUntil
    ) {
        return new WearAlertPolicy(low, high, noData, snoozedUntil);
    }
}
