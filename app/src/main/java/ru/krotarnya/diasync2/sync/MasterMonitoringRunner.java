package ru.krotarnya.diasync2.sync;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public final class MasterMonitoringRunner implements MonitoringRunner {
    private final Listener listener;
    private final CountDownLatch stopped = new CountDownLatch(1);

    public MasterMonitoringRunner(Listener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    @Override
    public void run() {
        listener.onStateChanged(SyncConnectionState.WAITING_FOR_XDRIP);
        try {
            stopped.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        stopped.countDown();
    }
}
