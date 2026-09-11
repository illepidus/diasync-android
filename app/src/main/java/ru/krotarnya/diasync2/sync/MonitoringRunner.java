package ru.krotarnya.diasync2.sync;

public interface MonitoringRunner extends Runnable {
    interface Listener {
        void onStateChanged(SyncConnectionState state);

        void onDataCommitted();
    }

    void stop();
}
