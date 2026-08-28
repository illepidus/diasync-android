package ru.krotarnya.diasync2.wear;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

final class WearGraphLayoutCalculator {
    WearGraphLayout calculate(WearComplicationState state, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Bitmap size must be positive");
        }
        float horizontalInset = Math.max(4f, width * 0.02f);
        float verticalInset = Math.max(4f, height * 0.035f);
        float statusSpace = Math.max(20f, height * 0.14f);
        WearGraphLayout.Bounds bounds = new WearGraphLayout.Bounds(
                horizontalInset,
                verticalInset,
                width - horizontalInset,
                height - verticalInset - statusSpace);

        double min = state.lowMgDl();
        double max = state.highMgDl();
        for (WearComplicationState.GraphPoint point : state.points()) {
            min = Math.min(min, point.displayMgDl());
            max = Math.max(max, point.displayMgDl());
        }
        double margin = Math.max(8.0, (max - min) * 0.08);
        double minYValue = Math.max(0.0, min - margin);
        double maxYValue = max + margin;
        long windowMillis = Duration.ofMinutes(state.graphWindowMinutes()).toMillis();
        long endMillis = state.renderedAt().toEpochMilli();
        long startMillis = endMillis - windowMillis;

        List<WearGraphLayout.Point> points = new ArrayList<>();
        for (WearComplicationState.GraphPoint point : state.points()) {
            double xFraction = (double) (point.timestamp().toEpochMilli() - startMillis)
                    / windowMillis;
            if (xFraction < 0.0 || xFraction > 1.0) {
                continue;
            }
            float x = (float) (bounds.left() + bounds.width() * xFraction);
            float y = toY(point.displayMgDl(), minYValue, maxYValue, bounds);
            points.add(new WearGraphLayout.Point(x, y, point.displayMgDl()));
        }

        float radius = Math.max(
                1.5f,
                Math.min(height * 0.025f, width * 100f / (state.graphWindowMinutes() * 60f)));

        return new WearGraphLayout(
                bounds,
                points,
                toY(state.lowMgDl(), minYValue, maxYValue, bounds),
                toY(state.highMgDl(), minYValue, maxYValue, bounds),
                radius);
    }

    private float toY(
            double value,
            double minValue,
            double maxValue,
            WearGraphLayout.Bounds bounds
    ) {
        double fraction = (value - minValue) / (maxValue - minValue);
        return (float) (bounds.bottom() - bounds.height() * fraction);
    }
}
