package ru.krotarnya.diasync2.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TrendIconRendererTest {
    private final TrendIconRenderer renderer = new TrendIconRenderer();

    @Test
    public void rendersEveryTrendAsAStableDistinctBitmap() {
        Set<Integer> signatures = new HashSet<>();

        for (String trend : List.of("⇊", "↓", "↘", "→", "↗", "↑", "⇈", "-")) {
            Bitmap bitmap = renderer.render(trend, 24, Color.WHITE);
            int[] pixels = new int[24 * 24];
            bitmap.getPixels(pixels, 0, 24, 0, 0, 24, 24);

            assertEquals(24, bitmap.getWidth());
            assertEquals(24, bitmap.getHeight());
            assertTrue(Arrays.stream(pixels).anyMatch(pixel -> Color.alpha(pixel) > 0));
            signatures.add(Arrays.hashCode(pixels));
        }

        assertEquals(8, signatures.size());
    }
}
