package ru.krotarnya.diasync2.master;

import ru.krotarnya.diasync2.settings.AppConfiguration;

public interface MasterUploadWork {
    MasterOutboxDrainer.Result drainOnce(
            AppConfiguration configuration,
            Runnable onUploadStarted);

    default MasterOutboxDrainer.Result drainOnce(AppConfiguration configuration) {
        return drainOnce(configuration, () -> { });
    }

    void cancelActiveCall();
}
