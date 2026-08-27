package ru.krotarnya.diasync2.settings;

import java.util.Objects;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public final class AppConfiguration {
    private final String baseUrl;
    private final String userId;
    private final GlucoseUnit unit;
    private final boolean useCalibration;

    public AppConfiguration(
            String baseUrl,
            String userId,
            GlucoseUnit unit,
            boolean useCalibration
    ) {
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.userId = Objects.requireNonNull(userId);
        this.unit = Objects.requireNonNull(unit);
        this.useCalibration = useCalibration;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String userId() {
        return userId;
    }

    public GlucoseUnit unit() {
        return unit;
    }

    public boolean useCalibration() {
        return useCalibration;
    }
}
