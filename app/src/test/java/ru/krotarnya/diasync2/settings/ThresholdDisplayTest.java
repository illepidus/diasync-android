package ru.krotarnya.diasync2.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import ru.krotarnya.diasync2.common.GlucoseUnit;

public class ThresholdDisplayTest {
    private final ThresholdDisplay display = new ThresholdDisplay();

    @Test
    public void formatsInternalThresholdsInSelectedUnit() {
        assertEquals("70", display.format(70.0, GlucoseUnit.MG_DL));
        assertEquals("3.9", display.format(70.0, GlucoseUnit.MMOL_L));
        assertEquals("180", display.format(180.0, GlucoseUnit.MG_DL));
        assertEquals("10.0", display.format(180.0, GlucoseUnit.MMOL_L));
    }

    @Test
    public void parsesDisplayedMmolThresholdIntoInternalMgDl() {
        assertEquals(70.2, display.parseMgDl("3.9", GlucoseUnit.MMOL_L, "Low"), 0.001);
        assertEquals(180.0, display.parseMgDl("180", GlucoseUnit.MG_DL, "High"), 0.001);
    }

    @Test
    public void rejectsNonPositiveAndMalformedThresholds() {
        assertThrows(IllegalArgumentException.class,
                () -> display.parseMgDl("0", GlucoseUnit.MMOL_L, "Low"));
        assertThrows(IllegalArgumentException.class,
                () -> display.parseMgDl("not a number", GlucoseUnit.MG_DL, "High"));
    }
}
