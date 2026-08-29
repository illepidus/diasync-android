package ru.krotarnya.diasync2.navigation;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class PhoneScreenTest {
    @Test public void missingOrInvalidDeepLinkReturnsToStatus() {
        assertEquals(PhoneScreen.STATUS, PhoneScreen.fromRoute(null));
        assertEquals(PhoneScreen.STATUS, PhoneScreen.fromRoute("unknown"));
    }

    @Test public void alertsDeepLinkIsStable() {
        assertEquals(PhoneScreen.ALERTS, PhoneScreen.fromRoute("ALERTS"));
    }
}
