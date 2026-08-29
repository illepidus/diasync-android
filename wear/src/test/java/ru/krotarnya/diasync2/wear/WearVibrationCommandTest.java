package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import ru.krotarnya.diasync2.common.AlertType;

public class WearVibrationCommandTest {
    @Test
    public void mapsLowHighAndNoDataCommands() {
        WearVibrationCommand low = WearVibrationCommand.forAlert(AlertType.LOW);
        WearVibrationCommand high = WearVibrationCommand.forAlert(AlertType.HIGH);
        WearVibrationCommand noData = WearVibrationCommand.forAlert(AlertType.NO_DATA);

        assertArrayEquals(new long[]{800L, 400L, 800L}, low.timings());
        assertArrayEquals(new int[]{255, 0, 255}, low.amplitudes());
        assertArrayEquals(new long[]{1_000L}, high.timings());
        assertArrayEquals(new int[]{255}, high.amplitudes());
        assertArrayEquals(new long[]{1_000L}, noData.timings());
        assertArrayEquals(new int[]{255}, noData.amplitudes());
    }
}
