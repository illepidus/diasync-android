package ru.krotarnya.diasync2.widget;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WidgetGraphRendererTest {
    private final WidgetGraphRenderer renderer = new WidgetGraphRenderer();

    @Test
    public void rendersConfiguredZonesAcrossTheBitmap() {
        WidgetGraphLayout layout = new WidgetGraphLayout(40, 100, 2.0f, 70.0, 30.0, List.of());

        Bitmap bitmap = renderer.render(layout, true, false);

        assertColorNear(WidgetGraphRenderer.HIGH_ZONE_COLOR, bitmap.getPixel(20, 10));
        assertColorNear(WidgetGraphRenderer.NORMAL_ZONE_COLOR, bitmap.getPixel(20, 50));
        assertColorNear(WidgetGraphRenderer.LOW_ZONE_COLOR, bitmap.getPixel(20, 90));
    }

    @Test
    public void rendersLowNormalAndHighPointsWithStableColors() {
        WidgetGraphLayout layout = new WidgetGraphLayout(
                60,
                30,
                2.0f,
                25.0,
                5.0,
                List.of(
                        new WidgetGraphLayout.Point(10.0f, 15.0f, WidgetState.Range.LOW),
                        new WidgetGraphLayout.Point(30.0f, 15.0f, WidgetState.Range.NORMAL),
                        new WidgetGraphLayout.Point(50.0f, 15.0f, WidgetState.Range.HIGH)));

        Bitmap bitmap = renderer.render(layout, false, false);

        assertEquals(WidgetGraphRenderer.LOW_COLOR, bitmap.getPixel(10, 15));
        assertEquals(WidgetGraphRenderer.NORMAL_COLOR, bitmap.getPixel(30, 15));
        assertEquals(WidgetGraphRenderer.HIGH_COLOR, bitmap.getPixel(50, 15));
    }

    @Test
    public void usesCanonicalCyanForNormalPoints() {
        assertEquals(Color.rgb(0, 191, 255), WidgetGraphRenderer.NORMAL_COLOR);
    }

    private void assertColorNear(int expected, int actual) {
        assertEquals(Color.alpha(expected), Color.alpha(actual));
        assertEquals(Color.red(expected), Color.red(actual), 5);
        assertEquals(Color.green(expected), Color.green(actual), 5);
        assertEquals(Color.blue(expected), Color.blue(actual), 5);
    }
}
