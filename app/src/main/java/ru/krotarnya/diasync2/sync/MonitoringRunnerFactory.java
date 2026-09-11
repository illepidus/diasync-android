package ru.krotarnya.diasync2.sync;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;
import ru.krotarnya.diasync2.master.MasterUploadWork;
import ru.krotarnya.diasync2.settings.AppConfiguration;

public final class MonitoringRunnerFactory {
    private MonitoringRunnerFactory() {
    }

    public static MonitoringRunner create(
            AppConfiguration configuration,
            Supplier<SyncWork> slaveWork,
            Supplier<MasterUploadWork> masterWork,
            BooleanSupplier networkValidated,
            MonitoringRunner.Listener listener
    ) {
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(slaveWork);
        Objects.requireNonNull(masterWork);
        Objects.requireNonNull(networkValidated);
        Objects.requireNonNull(listener);
        return switch (configuration.mode()) {
            case SLAVE -> new SyncRunner(
                    slaveWork.get(),
                    new BackoffPolicy(Math::random),
                    duration -> Thread.sleep(duration.toMillis()),
                    listener);
            case MASTER -> new MasterMonitoringRunner(
                    listener,
                    configuration,
                    masterWork.get(),
                    networkValidated);
        };
    }
}
