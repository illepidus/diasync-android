package ru.krotarnya.diasync2.wear;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.PutDataRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import ru.krotarnya.diasync2.common.AlertType;
import ru.krotarnya.diasync2.common.wear.WearAlertEvent;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;
import ru.krotarnya.diasync2.common.wear.WearSnapshotCodec;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AppPreferences;

public final class WearStatePublisher {
    private final AppPreferences preferences;
    private final WearSnapshotBuilder builder;
    private final WearSnapshotCodec codec;
    private final DataClient dataClient;
    private final WearDataRequestFactory requestFactory;
    private final WearSyncDiagnostics diagnostics;
    private final Executor executor;
    private final Clock clock;

    public WearStatePublisher(
            AppPreferences preferences,
            WearSnapshotBuilder builder,
            WearSnapshotCodec codec,
            DataClient dataClient,
            WearDataRequestFactory requestFactory,
            WearSyncDiagnostics diagnostics,
            Executor executor,
            Clock clock
    ) {
        this.preferences = Objects.requireNonNull(preferences);
        this.builder = Objects.requireNonNull(builder);
        this.codec = Objects.requireNonNull(codec);
        this.dataClient = Objects.requireNonNull(dataClient);
        this.requestFactory = Objects.requireNonNull(requestFactory);
        this.diagnostics = Objects.requireNonNull(diagnostics);
        this.executor = Objects.requireNonNull(executor);
        this.clock = Objects.requireNonNull(clock);
    }

    public void publishState() {
        publish(null);
    }

    public void publishAlert(AlertType type, Instant measurementTimestamp) {
        publish(WearAlertEvent.create(type, measurementTimestamp, clock.instant()));
    }

    private void publish(WearAlertEvent event) {
        executor.execute(() -> {
            Optional<AppConfiguration> configuration = preferences.load();
            if (configuration.isEmpty()) {
                return;
            }
            try {
                WearSnapshot snapshot = builder.build(
                        configuration.get(),
                        preferences.loadWatchSettings(),
                        preferences.loadAlertSettings(),
                        preferences.wearSnoozedUntil(),
                        event);
                byte[] payload = codec.encode(snapshot);
                PutDataRequest request = requestFactory.create(payload);
                diagnostics.record(WearSyncDiagnostics.State.SENDING, clock.instant());
                dataClient.putDataItem(request)
                        .addOnSuccessListener(ignored -> diagnostics.record(
                                WearSyncDiagnostics.State.SENT,
                                clock.instant()))
                        .addOnFailureListener(ignored -> diagnostics.record(
                                WearSyncDiagnostics.State.FAILED,
                                clock.instant()));
            } catch (RuntimeException exception) {
                diagnostics.record(WearSyncDiagnostics.State.FAILED, clock.instant());
            }
        });
    }
}
