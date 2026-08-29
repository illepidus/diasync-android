package ru.krotarnya.diasync2.sync;

public enum SyncConnectionState {
    DISABLED,
    CONNECTING,
    CONNECTED,
    RETRYING,
    BLOCKED
}
