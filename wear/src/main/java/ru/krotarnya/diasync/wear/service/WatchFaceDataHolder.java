package ru.krotarnya.diasync.wear.service;

import androidx.wear.watchface.Renderer;

import java.util.Optional;
import java.util.function.Consumer;

import lombok.Setter;
import ru.krotarnya.diasync.wear.model.WatchFaceData;
import ru.krotarnya.diasync.wear.render.DiasyncRenderer;

public final class WatchFaceDataHolder implements WatchFaceDataAccessor {
    private final WatchFaceData.WatchFaceDataBuilder builder = WatchFaceData.builder();

    @Setter
    private DiasyncRenderer renderer;

    @Override
    public WatchFaceData build() {
        return builder.build();
    }

    public void mutate(Consumer<WatchFaceData.WatchFaceDataBuilder> data) {
        data.accept(builder);
        Optional.ofNullable(renderer).ifPresent(Renderer::invalidate);
    }
}
