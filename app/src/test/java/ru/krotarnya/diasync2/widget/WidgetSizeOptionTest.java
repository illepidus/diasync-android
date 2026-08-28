package ru.krotarnya.diasync2.widget;

import static org.junit.Assert.assertEquals;

import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.util.SizeF;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WidgetSizeOptionTest {
    @Test
    public void convertsLauncherSizesDirectlyWithoutDeviceOrientation() {
        Bundle options = new Bundle();
        options.putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                new ArrayList<>(List.of(
                        new SizeF(120.0f, 80.0f),
                        new SizeF(240.0f, 70.0f))));

        List<WidgetSizeOption> sizes = WidgetSizeOption.from(options, 2.0f);

        assertEquals(2, sizes.size());
        assertEquals(new WidgetBitmapSize(240, 160, 120, 80), sizes.get(0).bitmapSize());
        assertEquals(new WidgetBitmapSize(480, 140, 240, 70), sizes.get(1).bitmapSize());
    }

    @Test
    public void deduplicatesAndBoundsLauncherSizes() {
        Bundle options = new Bundle();
        options.putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                new ArrayList<>(List.of(
                        new SizeF(100.0f, 100.0f),
                        new SizeF(100.0f, 100.0f),
                        new SizeF(200.0f, 100.0f),
                        new SizeF(300.0f, 100.0f),
                        new SizeF(400.0f, 100.0f),
                        new SizeF(500.0f, 100.0f))));

        assertEquals(4, WidgetSizeOption.from(options, 1.0f).size());
    }
}
