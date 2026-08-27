package ru.krotarnya.diasync2.widget;

import java.util.List;

public record WidgetGraphLayout(
        int width,
        int height,
        float pointRadius,
        double lowY,
        double highY,
        List<Point> points
) {
    public WidgetGraphLayout {
        points = List.copyOf(points);
    }

    public record Point(float x, float y, WidgetState.Range range) {
    }
}
