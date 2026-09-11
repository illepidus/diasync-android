package ru.krotarnya.diasync2.master;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Duration;
import org.junit.Test;

public class MasterRetryDelayTest {
    @Test
    public void exponentialDelayIsJitteredAndBounded() {
        MasterRetryDelay minimum = new MasterRetryDelay(() -> 0.0);
        MasterRetryDelay maximum = new MasterRetryDelay(() -> Math.nextDown(1.0));

        assertEquals(Duration.ofMillis(500), minimum.apply(1));
        assertEquals(Duration.ofSeconds(1), maximum.apply(1));
        assertEquals(Duration.ofSeconds(30), minimum.apply(100));
        assertEquals(MasterRetryDelay.MAX_DELAY, maximum.apply(100));
    }

    @Test
    public void rejectsInvalidRandomSamples() {
        assertThrows(IllegalStateException.class, () -> new MasterRetryDelay(() -> 1.0).apply(1));
    }
}
