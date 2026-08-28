package ru.krotarnya.diasync2.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Test;

public class SnoozeCountdownTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");
    private final SnoozeCountdown countdown = new SnoozeCountdown(
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    public void formatsRemainingTimeIncludingTwentyFourHours() {
        assertEquals("00:04:59", countdown.remaining(NOW.plusSeconds(299)).orElseThrow());
        assertEquals("01:02:03", countdown.remaining(NOW.plusSeconds(3_723)).orElseThrow());
        assertEquals("24:00:00", countdown.remaining(NOW.plusSeconds(86_400)).orElseThrow());
    }

    @Test
    public void hidesStatusAtAndAfterExpiry() {
        assertTrue(countdown.remaining(NOW).isEmpty());
        assertTrue(countdown.remaining(NOW.minusSeconds(1)).isEmpty());
    }
}
