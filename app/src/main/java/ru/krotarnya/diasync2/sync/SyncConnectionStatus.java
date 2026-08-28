package ru.krotarnya.diasync2.sync;

final class SyncConnectionStatus {
    private SyncConnectionStatus() {
    }

    static SyncConnectionState effective(SyncConnectionState runnerState, boolean networkValidated) {
        if (runnerState == SyncConnectionState.CONNECTED && !networkValidated) {
            return SyncConnectionState.CONNECTING;
        }
        return runnerState;
    }
}
