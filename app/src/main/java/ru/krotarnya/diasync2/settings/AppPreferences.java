package ru.krotarnya.diasync2.settings;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Optional;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public final class AppPreferences {
    private static final String FILE_NAME = "diasync_settings";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_UNIT = "unit";
    private static final String KEY_USE_CALIBRATION = "use_calibration";

    private final SharedPreferences preferences;

    public AppPreferences(Context context) {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public Optional<AppConfiguration> load() {
        String baseUrl = preferences.getString(KEY_BASE_URL, "");
        String userId = preferences.getString(KEY_USER_ID, "");
        if (baseUrl.isBlank() || userId.isBlank()) {
            return Optional.empty();
        }
        GlucoseUnit unit;
        try {
            unit = GlucoseUnit.valueOf(
                    preferences.getString(KEY_UNIT, GlucoseUnit.MMOL_L.name()));
        } catch (IllegalArgumentException exception) {
            unit = GlucoseUnit.MMOL_L;
        }
        return Optional.of(new AppConfiguration(
                baseUrl,
                userId,
                unit,
                preferences.getBoolean(KEY_USE_CALIBRATION, true)));
    }

    public void save(AppConfiguration configuration) {
        preferences.edit()
                .putString(KEY_BASE_URL, configuration.baseUrl())
                .putString(KEY_USER_ID, configuration.userId())
                .putString(KEY_UNIT, configuration.unit().name())
                .putBoolean(KEY_USE_CALIBRATION, configuration.useCalibration())
                .apply();
    }
}
