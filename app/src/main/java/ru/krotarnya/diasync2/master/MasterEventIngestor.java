package ru.krotarnya.diasync2.master;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import ru.krotarnya.diasync2.common.master.XdripEvent;
import ru.krotarnya.diasync2.common.master.XdripEventCodec;
import ru.krotarnya.diasync2.data.local.MasterEventDao;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AppMode;

public final class MasterEventIngestor {
    public enum Result {
        ACCEPTED,
        DUPLICATE,
        HASH_CONFLICT,
        REJECTED,
        FAILED
    }

    private final BooleanSupplier monitoringEnabled;
    private final Supplier<Optional<AppConfiguration>> configurationSupplier;
    private final XdripEventCodec codec;
    private final MasterEventRepository repository;
    private final Runnable dataCommitted;
    private final Runnable wakeUploader;
    private final Consumer<String> diagnostic;

    public MasterEventIngestor(
            BooleanSupplier monitoringEnabled,
            Supplier<Optional<AppConfiguration>> configurationSupplier,
            XdripEventCodec codec,
            MasterEventRepository repository,
            Runnable dataCommitted,
            Runnable wakeUploader,
            Consumer<String> diagnostic
    ) {
        this.monitoringEnabled = Objects.requireNonNull(monitoringEnabled);
        this.configurationSupplier = Objects.requireNonNull(configurationSupplier);
        this.codec = Objects.requireNonNull(codec);
        this.repository = Objects.requireNonNull(repository);
        this.dataCommitted = Objects.requireNonNull(dataCommitted);
        this.wakeUploader = Objects.requireNonNull(wakeUploader);
        this.diagnostic = Objects.requireNonNull(diagnostic);
    }

    public Result ingest(String payload) {
        Optional<AppConfiguration> savedConfiguration = configurationSupplier.get();
        if (!monitoringEnabled.getAsBoolean()
                || savedConfiguration.isEmpty()
                || savedConfiguration.get().mode() != AppMode.MASTER) {
            diagnostic.accept("IGNORED_INACTIVE_MASTER");
            return Result.REJECTED;
        }
        try {
            byte[] incoming = payload == null
                    ? null
                    : payload.getBytes(StandardCharsets.UTF_8);
            XdripEvent event = codec.decode(incoming);
            byte[] canonical = codec.encode(event);
            MasterEventDao.Acceptance acceptance = repository.accept(
                    event,
                    canonical,
                    savedConfiguration.get());
            return switch (acceptance) {
                case ACCEPTED -> accepted();
                case DUPLICATE -> Result.DUPLICATE;
                case HASH_CONFLICT -> {
                    diagnostic.accept("EVENT_ID_HASH_CONFLICT");
                    yield Result.HASH_CONFLICT;
                }
            };
        } catch (IllegalArgumentException exception) {
            diagnostic.accept("INVALID_EVENT");
            return Result.REJECTED;
        } catch (RuntimeException exception) {
            diagnostic.accept("PERSIST_FAILED");
            return Result.FAILED;
        }
    }

    private Result accepted() {
        runPostCommit(dataCommitted, "COORDINATOR_FAILED");
        runPostCommit(wakeUploader, "UPLOADER_WAKE_FAILED");
        return Result.ACCEPTED;
    }

    private void runPostCommit(Runnable action, String failureCode) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            diagnostic.accept(failureCode);
        }
    }
}
