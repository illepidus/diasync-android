package ru.krotarnya.diasync2.sync;

import java.time.Instant;
import ru.krotarnya.diasync2.data.BootstrapResult;
import ru.krotarnya.diasync2.data.LongPollResult;

public interface SyncWork {
    BootstrapResult bootstrap();

    default BootstrapResult reconcile() {
        return bootstrap();
    }

    LongPollResult poll(Instant since);

    void cancelActiveCall();
}
