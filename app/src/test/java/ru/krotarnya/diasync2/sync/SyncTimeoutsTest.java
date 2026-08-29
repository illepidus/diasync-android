package ru.krotarnya.diasync2.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Duration;
import org.junit.Test;

public class SyncTimeoutsTest {
    @Test
    public void debugBuildUsesShortDeviceScenarioTimeouts() {
        assertEquals(Duration.ofSeconds(10), SyncTimeouts.forBuild(true).serverLongPoll());
        assertEquals(Duration.ofSeconds(15), SyncTimeouts.forBuild(true).clientRead());
        assertEquals(Duration.ofSeconds(75), SyncTimeouts.forBuild(false).serverLongPoll());
        assertEquals(Duration.ofSeconds(90), SyncTimeouts.forBuild(false).clientRead());
    }

    @Test
    public void clientTimeoutMustExceedServerTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new SyncTimeouts(
                Duration.ofSeconds(10),
                Duration.ofSeconds(10)));
    }
}
