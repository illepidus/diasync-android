package ru.krotarnya.diasync2.sync;

public enum SyncConnectionState {
    DISABLED,
    CONNECTING,
    CONNECTED,
    WAITING_FOR_XDRIP,
    UPLOADING,
    RETRYING,
    BLOCKED
}
