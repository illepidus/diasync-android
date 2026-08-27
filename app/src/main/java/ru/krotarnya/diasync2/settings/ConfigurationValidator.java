package ru.krotarnya.diasync2.settings;

import okhttp3.HttpUrl;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public final class ConfigurationValidator {
    public AppConfiguration validate(
            String baseUrlInput,
            String userIdInput,
            GlucoseUnit unit,
            boolean useCalibration
    ) {
        String baseUrl = baseUrlInput.trim();
        String userId = userIdInput.trim();
        HttpUrl parsed = HttpUrl.parse(baseUrl);
        if (parsed == null || !"https".equals(parsed.scheme())) {
            throw new IllegalArgumentException("Backend URL must be a valid HTTPS URL");
        }
        if (userId.isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        return new AppConfiguration(baseUrl, userId, unit, useCalibration);
    }
}
