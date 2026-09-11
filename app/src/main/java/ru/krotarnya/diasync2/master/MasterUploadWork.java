package ru.krotarnya.diasync2.master;

import ru.krotarnya.diasync2.settings.AppConfiguration;

public interface MasterUploadWork {
    MasterOutboxDrainer.Result drainOnce(AppConfiguration configuration);

    void cancelActiveCall();
}
