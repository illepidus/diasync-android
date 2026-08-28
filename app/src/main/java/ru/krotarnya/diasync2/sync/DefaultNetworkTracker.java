package ru.krotarnya.diasync2.sync;

final class DefaultNetworkTracker {
    private static final long NO_NETWORK = 0L;

    private long networkHandle;
    private boolean validated;

    DefaultNetworkTracker(long networkHandle, boolean validated) {
        this.networkHandle = networkHandle;
        this.validated = networkHandle != NO_NETWORK && validated;
    }

    synchronized boolean onAvailable(long availableHandle, boolean availableValidated) {
        networkHandle = availableHandle;
        validated = availableValidated;
        return validated;
    }

    synchronized boolean onCapabilitiesChanged(long changedHandle, boolean changedValidated) {
        if (changedHandle == networkHandle) {
            validated = changedValidated;
        }
        return validated;
    }

    synchronized boolean onLost(long lostHandle) {
        if (lostHandle == networkHandle) {
            networkHandle = NO_NETWORK;
            validated = false;
        }
        return validated;
    }

    synchronized boolean isValidated() {
        return validated;
    }
}
