package ru.krotarnya.diasync2.settings;

import okhttp3.HttpUrl;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public final class ConfigurationValidator {
    public AppConfiguration validate(
            String baseUrlInput,
            String userIdInput,
            GlucoseUnit unit,
            boolean useCalibration,
            String lowMgDlInput,
            String highMgDlInput,
            GraphWindow widgetGraphWindow,
            boolean widgetGraphZones,
            boolean widgetGraphLines,
            boolean widgetTrendArrow
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
        double lowMgDl = parseThreshold(lowMgDlInput, "Low threshold");
        double highMgDl = parseThreshold(highMgDlInput, "High threshold");
        if (lowMgDl >= highMgDl) {
            throw new IllegalArgumentException("Low threshold must be below high threshold");
        }
        return new AppConfiguration(
                baseUrl,
                userId,
                unit,
                useCalibration,
                lowMgDl,
                highMgDl,
                widgetGraphWindow,
                widgetGraphZones,
                widgetGraphLines,
                widgetTrendArrow);
    }

    private double parseThreshold(String input, String name) {
        try {
            double value = Double.parseDouble(input.trim());
            if (!Double.isFinite(value) || value <= 0.0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a positive mg/dL number");
        }
    }
}
