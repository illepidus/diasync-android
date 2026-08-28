package ru.krotarnya.diasync2.wear;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;
import ru.krotarnya.diasync2.common.wear.WearSnapshotCodec;

public final class LastKnownWearStateRepository {
    private final WearSnapshotStore store;
    private final WearSnapshotCodec codec;

    LastKnownWearStateRepository(WearSnapshotStore store, WearSnapshotCodec codec) {
        this.store = Objects.requireNonNull(store);
        this.codec = Objects.requireNonNull(codec);
    }

    public static LastKnownWearStateRepository create(android.content.Context context) {
        return new LastKnownWearStateRepository(
                new SharedPreferencesWearSnapshotStore(context),
                new WearSnapshotCodec());
    }

    public boolean replaceIfValid(byte[] payload) {
        try {
            WearSnapshot validated = codec.decode(payload);
            byte[] canonicalPayload = codec.encode(validated);
            byte[] current = store.read();
            return Arrays.equals(current, canonicalPayload) || store.write(canonicalPayload);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public Optional<WearSnapshot> load() {
        byte[] payload = store.read();
        if (payload == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(codec.decode(payload));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
