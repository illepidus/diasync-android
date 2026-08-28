package ru.krotarnya.diasync2.sync;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DefaultNetworkTrackerTest {
    @Test
    public void losingCurrentNetworkInvalidatesWithoutASecondCapabilitiesQuery() {
        DefaultNetworkTracker tracker = new DefaultNetworkTracker(11L, true);

        assertFalse(tracker.onLost(11L));
        assertFalse(tracker.isValidated());
    }

    @Test
    public void lateEventFromOldNetworkCannotInvalidateReplacement() {
        DefaultNetworkTracker tracker = new DefaultNetworkTracker(11L, true);

        assertTrue(tracker.onAvailable(22L, true));
        assertTrue(tracker.onLost(11L));
        assertTrue(tracker.onCapabilitiesChanged(11L, false));
        assertTrue(tracker.isValidated());
    }

    @Test
    public void capabilitiesChangeAppliesOnlyToCurrentDefaultNetwork() {
        DefaultNetworkTracker tracker = new DefaultNetworkTracker(11L, false);

        assertTrue(tracker.onCapabilitiesChanged(11L, true));
        assertFalse(tracker.onCapabilitiesChanged(11L, false));
    }
}
