package ru.krotarnya.diasync2.sync;

import java.time.Instant;
import java.util.Objects;
import okhttp3.OkHttpClient;
import ru.krotarnya.diasync2.data.BootstrapRepository;
import ru.krotarnya.diasync2.data.BootstrapResult;
import ru.krotarnya.diasync2.data.LongPollRepository;
import ru.krotarnya.diasync2.data.LongPollResult;
import ru.krotarnya.diasync2.settings.AppConfiguration;

public final class RepositorySyncWork implements SyncWork {
    private final AppConfiguration configuration;
    private final BootstrapRepository bootstrapRepository;
    private final LongPollRepository longPollRepository;
    private final OkHttpClient httpClient;

    public RepositorySyncWork(
            AppConfiguration configuration,
            BootstrapRepository bootstrapRepository,
            LongPollRepository longPollRepository,
            OkHttpClient httpClient
    ) {
        this.configuration = Objects.requireNonNull(configuration);
        this.bootstrapRepository = Objects.requireNonNull(bootstrapRepository);
        this.longPollRepository = Objects.requireNonNull(longPollRepository);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public BootstrapResult bootstrap() {
        return bootstrapRepository.bootstrap(configuration.baseUrl(), configuration.userId());
    }

    @Override
    public BootstrapResult reconcile() {
        return bootstrapRepository.reconcile(configuration.baseUrl(), configuration.userId());
    }

    @Override
    public LongPollResult poll(Instant since) {
        return longPollRepository.poll(
                configuration.baseUrl(),
                configuration.userId(),
                since);
    }

    @Override
    public void cancelActiveCall() {
        longPollRepository.cancelActiveCall();
        httpClient.dispatcher().cancelAll();
    }
}
