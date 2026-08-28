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
        return scaled(widthDp, heightDp, density);
    }

    static WidgetBitmapSize exact(int widthDp, int heightDp, float density) {
        return scaled(widthDp, heightDp, density);
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

    private static WidgetBitmapSize scaled(int widthDp, int heightDp, float density) {
        int safeWidthDp = widthDp > 0 ? widthDp : 1;
        int safeHeightDp = heightDp > 0 ? heightDp : 1;
        float safeDensity = Float.isFinite(density) && density > 0.0f ? density : 0.1f;
        double rawWidth = (double) safeWidthDp * safeDensity;
        double rawHeight = (double) safeHeightDp * safeDensity;
        double scale = Math.min(
                1.0,
                MAX_DIMENSION_PX / Math.max(rawWidth, rawHeight));
        int width = (int) Math.max(1, Math.round(rawWidth * scale));
        int height = (int) Math.max(1, Math.round(rawHeight * scale));
        return new WidgetBitmapSize(width, height, widthDp, heightDp);
    }
}
