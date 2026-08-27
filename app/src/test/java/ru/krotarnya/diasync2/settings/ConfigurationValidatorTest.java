package ru.krotarnya.diasync2.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public class ConfigurationValidatorTest {
    private final ConfigurationValidator validator = new ConfigurationValidator();

    @Test
    public void acceptsWidgetThresholdAndGraphSettings() {
        AppConfiguration configuration = validator.validate(
                "https://example.test",
                "secret",
                GlucoseUnit.MMOL_L,
                true,
                "75",
                "170.5",
                GraphWindow.ONE_HOUR,
                false,
                true,
                false);

        assertEquals(75.0, configuration.lowMgDl(), 0.0);
        assertEquals(170.5, configuration.highMgDl(), 0.0);
        assertEquals(GraphWindow.ONE_HOUR, configuration.widgetGraphWindow());
        assertFalse(configuration.widgetTrendArrow());
    }

    @Test
    public void rejectsReversedThresholds() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
                "https://example.test",
                "secret",
                GlucoseUnit.MMOL_L,
                true,
                "180",
                "70",
                GraphWindow.THIRTY_MINUTES,
                true,
                false,
                true));
    }
}
