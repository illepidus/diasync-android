package ru.krotarnya.diasync2.common;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class TrendCalculator {
    private static final Duration WINDOW = Duration.ofMinutes(10);

    public String calculate(List<SensorPoint> points, boolean useCalibration) {
        Objects.requireNonNull(points);
        SensorPoint latest = points.stream()
                .max(Comparator.comparing(SensorPoint::timestamp))
                .orElse(null);
        if (latest == null) {
            return "";
        }

        Instant windowStart = latest.timestamp().minus(WINDOW);
        double sum = 0.0;
        int count = 0;
        for (SensorPoint point : points) {
            if (point.timestamp().isBefore(latest.timestamp())
                    && !point.timestamp().isBefore(windowStart)) {
                sum += point.displayValue(useCalibration).mgDl();
                count++;
            }
        }
        if (count == 0) {
            return "";
        }

        double delta = latest.displayValue(useCalibration).mgDl() - sum / count;
        if (delta <= -13.5) {
            return "⇊";
        }
        if (delta <= -7.0) {
            return "↓";
        }
        if (delta <= -3.0) {
            return "↘";
        }
        if (delta <= 3.0) {
            return "→";
        }
        if (delta <= 7.0) {
            return "↗";
        }
        if (delta <= 13.5) {
            return "↑";
        }
        return "⇈";
    }
}
