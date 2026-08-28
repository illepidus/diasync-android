package ru.krotarnya.diasync2.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WidgetBitmapSizeTest {
    @Test
    public void convertsWidgetDpOptionsToPixels() {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 72);

        WidgetBitmapSize size = WidgetBitmapSize.from(options, 2.0f, true);

        assertEquals(360, size.width());
        assertEquals(144, size.height());
        assertEquals(180, size.widthDp());
        assertEquals(72, size.heightDp());
        assertTrue(size.valueTextSp() > 22.0f);
    }

    @Test
    public void proportionallyCapsLargeOrInvalidLauncherDimensions() {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, Integer.MAX_VALUE);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);

        WidgetBitmapSize size = WidgetBitmapSize.from(options, 4.0f, false);

        assertEquals(WidgetBitmapSize.MAX_DIMENSION_PX, size.width());
        assertEquals(1, size.height());
    }

    @Test
    public void preservesAspectRatioWhenWideBitmapExceedsPixelLimit() {
        WidgetBitmapSize size = WidgetBitmapSize.exact(1200, 200, 2.0f);

        assertEquals(WidgetBitmapSize.MAX_DIMENSION_PX, size.width());
        assertEquals(171, size.height());
        assertEquals(1200.0 / 200.0, (double) size.width() / size.height(), 0.02);
    }

    @Test
    public void pairsDimensionsForTheCurrentOrientation() {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 120);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 240);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160);

        WidgetBitmapSize portrait = WidgetBitmapSize.from(options, 2.0f, true);
        WidgetBitmapSize landscape = WidgetBitmapSize.from(options, 2.0f, false);

        assertEquals(240, portrait.width());
        assertEquals(320, portrait.height());
        assertEquals(480, landscape.width());
        assertEquals(160, landscape.height());
    }

    @Test
    public void presentationMetricsStayReadableAtOneCellAndGrowWithArea() {
        WidgetBitmapSize oneCell = new WidgetBitmapSize(70, 70, 70, 70);
        WidgetBitmapSize medium = new WidgetBitmapSize(110, 300, 110, 300);
        WidgetBitmapSize large = new WidgetBitmapSize(400, 500, 400, 500);

        assertEquals(22.0f, oneCell.valueTextSp(), 0.001f);
        assertTrue(medium.valueTextSp() > oneCell.valueTextSp());
        assertTrue(large.valueTextSp() > medium.valueTextSp());
        assertEquals(48.4f, large.valueTextSp(), 0.001f);
        assertTrue(medium.trendIconDp() > oneCell.trendIconDp());
    }
}
