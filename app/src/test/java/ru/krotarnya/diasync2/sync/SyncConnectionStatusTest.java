package ru.krotarnya.diasync2.sync;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SyncConnectionStatusTest {
    @Test
    public void connectedRequiresAValidatedNetwork() {
        assertEquals(
                SyncConnectionState.CONNECTED,
                SyncConnectionStatus.effective(SyncConnectionState.CONNECTED, true));
        assertEquals(
                SyncConnectionState.CONNECTING,
                SyncConnectionStatus.effective(SyncConnectionState.CONNECTED, false));
    }

    @Test
    public void validatedNetworkDoesNotHideConnectingState() {
        assertEquals(
                SyncConnectionState.CONNECTING,
                SyncConnectionStatus.effective(SyncConnectionState.CONNECTING, true));
    }
}
