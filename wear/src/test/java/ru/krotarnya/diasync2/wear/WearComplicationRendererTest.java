package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WearComplicationRendererTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");
    private final WearComplicationRenderer renderer = new WearComplicationRenderer();

    @Test
    public void normalBitmapHasExactSizeBlackBackgroundAndVisibleContent() {
        Bitmap bitmap = renderer.render(state(110.0, "→", NOW.minusSeconds(30)), 360, 235);

        assertEquals(360, bitmap.getWidth());
        assertEquals(235, bitmap.getHeight());
        assertEquals(Color.BLACK, bitmap.getPixel(0, 0));
        assertTrue(countNonBlackPixels(bitmap) > 500);
    }

    @Test
    public void lowHighAndStaleRenderVisiblePixelsWhileErrorKeepsExactCanvas() {
        for (WearComplicationState state : List.of(
                state(62.0, "↓", NOW.minusSeconds(30)),
                state(205.0, "↑", NOW.minusSeconds(30)),
                state(110.0, "→", NOW.minusSeconds(121)))) {
            assertTrue(countNonBlackPixels(renderer.render(state, 360, 235)) > 100);
        }
        Bitmap error = renderer.render(
                new WearComplicationStateMapper(Clock.fixed(NOW, ZoneOffset.UTC))
                        .map(Optional.empty()),
                360,
                235);
        assertEquals(360, error.getWidth());
        assertEquals(235, error.getHeight());
    }

    private WearComplicationState state(double value, String trend, Instant timestamp) {
        WearSnapshot snapshot = new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                NOW,
                List.of(
                        new WearGlucosePoint(timestamp, value, null, null),
                        new WearGlucosePoint(timestamp.minusSeconds(300), 110.0, null, null)),
                new WearDisplayPolicy(
                        GlucoseUnit.MG_DL,
                        true,
                        70.0,
                        180.0,
                        30,
                        true,
                        true,
                        true,
                        trend),
                new WearAlertPolicy(false, false, false, Instant.EPOCH),
                null);
        return new WearComplicationStateMapper(Clock.fixed(NOW, ZoneOffset.UTC))
                .map(Optional.of(snapshot));
    }

    private int countNonBlackPixels(Bitmap bitmap) {
        int count = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                if (bitmap.getPixel(x, y) != Color.BLACK) {
                    count++;
                }
            }
        }
        return count;
    }
}
