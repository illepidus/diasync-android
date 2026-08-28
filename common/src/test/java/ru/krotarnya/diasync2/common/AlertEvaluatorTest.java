package ru.krotarnya.diasync2.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;

public class AlertEvaluatorTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");
    private static final AlertPolicy ALL_ENABLED = new AlertPolicy(true, true, true, 70.0, 180.0);

    private final MutableClock clock = new MutableClock(NOW);
    private final AlertEvaluator evaluator = new AlertEvaluator(clock);

    @Test
    public void lowRequiresThresholdAndStrictWorsening() {
        assertAlert(AlertType.LOW, reading(70.0), reading(71.0), ALL_ENABLED);
        advancePastThrottle();
        assertAlert(AlertType.LOW, reading(69.0), null, ALL_ENABLED);
        advancePastThrottle();
        assertNoAlert(reading(70.1), reading(71.0), ALL_ENABLED);
        assertNoAlert(reading(70.0), reading(70.0), ALL_ENABLED);
        assertNoAlert(reading(70.0), reading(69.0), ALL_ENABLED);
        assertNoAlert(reading(69.0), reading(71.0), policy(false, true, true));
    }

    @Test
    public void highRequiresThresholdAndStrictWorsening() {
        assertAlert(AlertType.HIGH, reading(180.0), reading(179.0), ALL_ENABLED);
        advancePastThrottle();
        assertAlert(AlertType.HIGH, reading(181.0), null, ALL_ENABLED);
        advancePastThrottle();
        assertNoAlert(reading(179.9), reading(179.0), ALL_ENABLED);
        assertNoAlert(reading(180.0), reading(180.0), ALL_ENABLED);
        assertNoAlert(reading(180.0), reading(181.0), ALL_ENABLED);
        assertNoAlert(reading(181.0), reading(179.0), policy(true, false, true));
    }

    @Test
    public void noDataTriggersForMissingOrExactlyFiveMinuteOldPoint() {
        assertAlert(AlertType.NO_DATA, null, null, ALL_ENABLED);
        advancePastThrottle();
        assertNoAlert(readingAt(100.0, clock.instant().minus(Duration.ofMinutes(5)).plusNanos(1)),
                null, ALL_ENABLED);
        assertAlert(AlertType.NO_DATA,
                readingAt(100.0, clock.instant().minus(Duration.ofMinutes(5))),
                null,
                ALL_ENABLED);
        advancePastThrottle();
        assertNoAlert(null, null, policy(true, true, false));
    }

    @Test
    public void lowAndHighTakePriorityOverNoData() {
        AlertReading staleLow = readingAt(60.0, NOW.minus(Duration.ofMinutes(10)));
        assertAlert(AlertType.LOW, staleLow, reading(65.0), ALL_ENABLED);

        advancePastThrottle();
        AlertReading staleHigh = readingAt(190.0, clock.instant().minus(Duration.ofMinutes(10)));
        assertAlert(AlertType.HIGH, staleHigh, reading(185.0), ALL_ENABLED);
    }

    @Test
    public void globalThrottleSuppressesAllTypesUntilFiftyFiveSeconds() {
        assertAlert(AlertType.LOW, reading(60.0), reading(65.0), ALL_ENABLED);
        clock.advance(Duration.ofSeconds(54));
        assertNoAlert(null, null, ALL_ENABLED);
        clock.advance(Duration.ofSeconds(1));
        assertAlert(AlertType.NO_DATA, null, null, ALL_ENABLED);
    }

    @Test
    public void restoredLastAlertPreservesThrottleAcrossProcessRestart() {
        assertAlert(AlertType.LOW, reading(60.0), reading(65.0), ALL_ENABLED);
        clock.advance(Duration.ofSeconds(30));
        AlertEvaluator restarted = new AlertEvaluator(clock, evaluator.lastAlertAt());

        assertFalse(restarted.evaluate(null, null, ALL_ENABLED, Instant.EPOCH).shouldAlert());

        clock.advance(Duration.ofSeconds(25));
        assertEquals(
                AlertType.NO_DATA,
                restarted.evaluate(null, null, ALL_ENABLED, Instant.EPOCH).type().orElseThrow());
    }

    @Test
    public void snoozeSuppressesAllTypesAndResumeIsImmediate() {
        Instant snoozedUntil = NOW.plus(Duration.ofHours(1));

        AlertDecision snoozed = evaluator.evaluate(
                reading(60.0), reading(65.0), ALL_ENABLED, snoozedUntil);
        AlertDecision resumed = evaluator.evaluate(
                reading(60.0), reading(65.0), ALL_ENABLED, Instant.EPOCH);

        assertFalse(snoozed.shouldAlert());
        assertEquals(AlertType.LOW, resumed.type().orElseThrow());
    }

    @Test
    public void freshNormalPointRecoversNoDataState() {
        assertAlert(AlertType.NO_DATA, null, null, ALL_ENABLED);
        advancePastThrottle();
        assertNoAlert(reading(100.0), reading(99.0), ALL_ENABLED);
        assertAlert(AlertType.NO_DATA, null, null, ALL_ENABLED);
    }

    private AlertPolicy policy(boolean low, boolean high, boolean noData) {
        return new AlertPolicy(low, high, noData, 70.0, 180.0);
    }

    private AlertReading reading(double value) {
        return readingAt(value, clock.instant());
    }

    private AlertReading readingAt(double value, Instant timestamp) {
        return new AlertReading(timestamp, value);
    }

    private void assertAlert(
            AlertType expected,
            AlertReading latest,
            AlertReading previous,
            AlertPolicy policy
    ) {
        AlertDecision decision = evaluator.evaluate(latest, previous, policy, Instant.EPOCH);
        assertTrue(decision.shouldAlert());
        assertEquals(expected, decision.type().orElseThrow());
    }

    private void assertNoAlert(
            AlertReading latest,
            AlertReading previous,
            AlertPolicy policy
    ) {
        assertFalse(evaluator.evaluate(latest, previous, policy, Instant.EPOCH).shouldAlert());
    }

    private void advancePastThrottle() {
        clock.advance(AlertEvaluator.SILENCE_INTERVAL);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
