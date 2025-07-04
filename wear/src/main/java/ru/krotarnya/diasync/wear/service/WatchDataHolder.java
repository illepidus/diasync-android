package ru.krotarnya.diasync.wear.service;

import ru.krotarnya.diasync.wear.model.WatchFace;

public class WatchDataHolder {
    private final WatchFace.WatchFaceBuilder dataBuilder = WatchFace.builder();
    
    public WatchFace get() {
        return dataBuilder.build();
    }

    public WatchFace.WatchFaceBuilder mutate() {
        return dataBuilder;
    }
}
