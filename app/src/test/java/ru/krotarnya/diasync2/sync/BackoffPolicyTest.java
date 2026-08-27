package ru.krotarnya.diasync2.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import org.junit.Test;

public class BackoffPolicyTest {
    @Test
    public void delayIsExponentiallyBoundedAndJittered() {
        BackoffPolicy minimumJitter = new BackoffPolicy(() -> 0.0);
        BackoffPolicy maximumJitter = new BackoffPolicy(() -> Math.nextDown(1.0));

        assertEquals(Duration.ofMillis(500), minimumJitter.nextDelay());
        assertEquals(Duration.ofMillis(1_000), maximumJitter.nextDelay());
        for (int index = 0; index < 20; index++) {
            Duration delay = maximumJitter.nextDelay();
            assertTrue(delay.compareTo(BackoffPolicy.MAX_DELAY) <= 0);
            assertTrue(delay.compareTo(Duration.ofSeconds(30)) >= 0 || index < 5);
        }
    }

    @Test
    public void resetReturnsToInitialDelay() {
        BackoffPolicy policy = new BackoffPolicy(() -> 0.0);
        policy.nextDelay();
        policy.nextDelay();

        policy.reset();

        assertEquals(Duration.ofMillis(500), policy.nextDelay());
    }
}
