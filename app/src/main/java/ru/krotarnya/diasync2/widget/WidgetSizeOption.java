package ru.krotarnya.diasync2.widget;

import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.util.SizeF;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

record WidgetSizeOption(SizeF hostSize, WidgetBitmapSize bitmapSize) {
    private static final int MAX_HOST_SIZES = 4;

    @SuppressWarnings("deprecation")
    static List<WidgetSizeOption> from(Bundle options, float density) {
        ArrayList<SizeF> provided = options.getParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES);
        if (provided == null || provided.isEmpty()) {
            return List.of();
        }
        Set<SizeF> unique = new LinkedHashSet<>(provided);
        List<WidgetSizeOption> result = new ArrayList<>(Math.min(unique.size(), MAX_HOST_SIZES));
        for (SizeF size : unique) {
            if (result.size() == MAX_HOST_SIZES) {
                break;
            }
            int widthDp = boundedDp(size.getWidth());
            int heightDp = boundedDp(size.getHeight());
            result.add(new WidgetSizeOption(
                    size,
                    WidgetBitmapSize.exact(widthDp, heightDp, density)));
        }
        return List.copyOf(result);
    }

    private static int boundedDp(float value) {
        return Float.isFinite(value) && value > 0.0f ? Math.round(value) : 1;
    }
}
