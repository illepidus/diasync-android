package ru.krotarnya.diasync2.sync;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import ru.krotarnya.diasync2.master.MasterOutboxDrainer;
import ru.krotarnya.diasync2.master.MasterUploadWork;
import ru.krotarnya.diasync2.settings.AppConfiguration;

public final class MasterMonitoringRunner implements MonitoringRunner {
    private final Listener listener;
    private final AppConfiguration configuration;
    private final MasterUploadWork uploadWork;
    private final BooleanSupplier networkValidated;
    private final Object signal = new Object();
    private volatile boolean stopped;

    public MasterMonitoringRunner(
            Listener listener,
            AppConfiguration configuration,
            MasterUploadWork uploadWork,
            BooleanSupplier networkValidated
    ) {
        this.listener = Objects.requireNonNull(listener);
        this.configuration = Objects.requireNonNull(configuration);
        this.uploadWork = Objects.requireNonNull(uploadWork);
        this.networkValidated = Objects.requireNonNull(networkValidated);
    }

    @Override
    public void run() {
        while (!stopped) {
            if (!networkValidated.getAsBoolean()) {
                listener.onStateChanged(SyncConnectionState.RETRYING);
                awaitSignal();
                continue;
            }
            listener.onStateChanged(SyncConnectionState.UPLOADING);
            MasterOutboxDrainer.Result result = uploadWork.drainOnce(configuration);
            if (result.kind() == MasterOutboxDrainer.Kind.DELIVERED) {
                continue;
            }
            listener.onStateChanged(switch (result.kind()) {
                case IDLE -> SyncConnectionState.WAITING_FOR_XDRIP;
                case RETRY_SCHEDULED -> SyncConnectionState.RETRYING;
                case BLOCKED -> SyncConnectionState.BLOCKED;
                case DELIVERED -> throw new IllegalStateException("Handled above");
            });
            awaitSignal();
        }
    }

    @Override
    public void stop() {
        stopped = true;
        uploadWork.cancelActiveCall();
        wake();
    }

    public void wake() {
        synchronized (signal) {
            signal.notifyAll();
        }
    }

    private void awaitSignal() {
        synchronized (signal) {
            if (stopped) {
                return;
            }
            try {
                signal.wait(1_000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                stopped = true;
            }
        }
    }
}
