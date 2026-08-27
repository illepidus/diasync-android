package ru.krotarnya.diasync2.widget;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WidgetGraphLayoutCalculator {
    static final double Y_MARGIN_MG_DL = 18.0;
    private static final Duration TIME_MARGIN = Duration.ofMinutes(1);

    public WidgetGraphLayout calculate(
            List<WidgetGraphSample> samples,
            Instant now,
            Duration window,
            double lowMgDl,
            double highMgDl,
            int width,
            int height
    ) {
        Objects.requireNonNull(samples);
        Objects.requireNonNull(now);
        Objects.requireNonNull(window);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Graph size must be positive");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Graph window must be positive");
        }

        Instant xMin = now.minus(window).minus(TIME_MARGIN);
        Instant xMax = now.plus(TIME_MARGIN);
        List<WidgetGraphSample> visible = new ArrayList<>();
        double dataMin = Math.min(lowMgDl, highMgDl);
        double dataMax = Math.max(lowMgDl, highMgDl);
        for (WidgetGraphSample sample : samples) {
            if (sample.timestamp().isBefore(xMin) || sample.timestamp().isAfter(xMax)) {
                continue;
            }
            visible.add(sample);
            dataMin = Math.min(dataMin, sample.mgDl());
            dataMax = Math.max(dataMax, sample.mgDl());
        }

        double yMin = dataMin - Y_MARGIN_MG_DL;
        double yMax = dataMax + Y_MARGIN_MG_DL;
        if (!(yMax > yMin)) {
            yMin = dataMin - 1.0;
            yMax = dataMax + 1.0;
        }

        long xSpanMillis = Duration.between(xMin, xMax).toMillis();
        float pointRadius = Math.min(
                (float) ((double) width * 25_000.0 / xSpanMillis),
                (float) height / 50.0f);
        float left = pointRadius;
        float right = Math.max(left, width - left);
        float top = pointRadius;
        float bottom = Math.max(top, height - top);
        List<WidgetGraphLayout.Point> points = new ArrayList<>(visible.size());
        for (WidgetGraphSample sample : visible) {
            double xRatio = (double) Duration.between(xMin, sample.timestamp()).toMillis()
                    / xSpanMillis;
            double yRatio = (sample.mgDl() - yMin) / (yMax - yMin);
            points.add(new WidgetGraphLayout.Point(
                    interpolate(left, right, xRatio),
                    interpolate(bottom, top, yRatio),
                    range(sample.mgDl(), lowMgDl, highMgDl)));
        }
        return new WidgetGraphLayout(
                width,
                height,
                pointRadius,
                mapY(lowMgDl, yMin, yMax, top, bottom),
                mapY(highMgDl, yMin, yMax, top, bottom),
                points);
    }

    private float mapY(double value, double min, double max, float top, float bottom) {
        return interpolate(bottom, top, (value - min) / (max - min));
    }

    private float interpolate(float start, float end, double ratio) {
        double bounded = Math.max(0.0, Math.min(1.0, ratio));
        return (float) (start + (end - start) * bounded);
    }

    private WidgetState.Range range(double mgDl, double lowMgDl, double highMgDl) {
        if (mgDl <= lowMgDl) {
            return WidgetState.Range.LOW;
        }
        if (mgDl >= highMgDl) {
            return WidgetState.Range.HIGH;
        }
        return WidgetState.Range.NORMAL;
    }
}
