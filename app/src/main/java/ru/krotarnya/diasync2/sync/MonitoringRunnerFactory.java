package ru.krotarnya.diasync2.sync;

import java.util.Objects;
import java.util.function.Supplier;
import ru.krotarnya.diasync2.settings.AppMode;

public final class MonitoringRunnerFactory {
    private MonitoringRunnerFactory() {
    }

    public static MonitoringRunner create(
            AppMode mode,
            Supplier<SyncWork> slaveWork,
            MonitoringRunner.Listener listener
    ) {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(slaveWork);
        Objects.requireNonNull(listener);
        return switch (mode) {
            case SLAVE -> new SyncRunner(
                    slaveWork.get(),
                    new BackoffPolicy(Math::random),
                    duration -> Thread.sleep(duration.toMillis()),
                    listener);
            case MASTER -> new MasterMonitoringRunner(listener);
        };
    }
}
