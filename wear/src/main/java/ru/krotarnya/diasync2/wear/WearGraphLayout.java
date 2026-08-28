package ru.krotarnya.diasync2.wear;

import java.util.List;

record WearGraphLayout(Bounds bounds, List<Point> points, float lowY, float highY, float pointRadius) {
    record Bounds(float left, float top, float right, float bottom) {
        float width() {
            return right - left;
        }

        float height() {
            return bottom - top;
        }

        float centerY() {
            return (top + bottom) / 2f;
        }

        boolean contains(float x, float y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }
    }

    record Point(float x, float y, double displayMgDl) {
    }

    WearGraphLayout {
        points = List.copyOf(points);
    }
}
