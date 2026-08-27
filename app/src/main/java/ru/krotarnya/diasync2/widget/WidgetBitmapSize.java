package ru.krotarnya.diasync2.widget;

import android.appwidget.AppWidgetManager;
import android.os.Bundle;

record WidgetBitmapSize(int width, int height, int widthDp, int heightDp) {
    static final int MAX_DIMENSION_PX = 1024;
    private static final int DEFAULT_WIDTH_DP = 180;
    private static final int DEFAULT_HEIGHT_DP = 110;

    static WidgetBitmapSize from(Bundle options, float density, boolean portrait) {
        int widthDp = portrait
                ? option(options,
                        AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                        AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                        DEFAULT_WIDTH_DP)
                : option(options,
                        AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                        AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                        DEFAULT_WIDTH_DP);
        int heightDp = portrait
                ? option(options,
                        AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                        AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                        DEFAULT_HEIGHT_DP)
                : option(options,
                        AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                        AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                        DEFAULT_HEIGHT_DP);
        return new WidgetBitmapSize(
                toBoundedPixels(widthDp, density),
                toBoundedPixels(heightDp, density),
                widthDp,
                heightDp);
    }

    private static int option(Bundle options, String primary, String fallback, int defaultValue) {
        if (options.containsKey(primary)) {
            return options.getInt(primary);
        }
        return options.getInt(fallback, defaultValue);
    }

    float valueTextSp() {
        return 22.0f * presentationScale();
    }

    float trendIconDp() {
        return 16.0f * presentationScale();
    }

    float messageTextSp() {
        return 10.0f * Math.min(presentationScale(), 1.6f);
    }

    private float presentationScale() {
        double area = Math.max(1.0, widthDp) * Math.max(1.0, heightDp);
        return (float) Math.max(1.0, Math.min(2.2, Math.sqrt(area / (110.0 * 110.0))));
    }

    private static int toBoundedPixels(int dp, float density) {
        int fallback = dp > 0 ? dp : 1;
        long pixels = Math.round((double) fallback * Math.max(density, 0.1f));
        return (int) Math.max(1, Math.min(MAX_DIMENSION_PX, pixels));
    }
}
