package ru.krotarnya.diasync.wear.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Data;
import ru.krotarnya.diasync.common.repository.DataPoint;
import ru.krotarnya.diasync.common.repository.Settings;
import ru.krotarnya.diasync.common.util.DateTimeUtils;
import ru.krotarnya.diasync.wear.model.WatchFace;

public final class ChartRenderer implements ComponentRenderer {
    private static final String TAG = ChartRenderer.class.getSimpleName();

    private static final Duration STALE_INTERVAL = Duration.ofMinutes(10);

    private static final int GRID_COLOR = Color.GRAY;
    private static final int OUTLINE_COLOR = Color.BLACK;
    private static final int SECONDARY_TEXT_COLOR = GRID_COLOR;

    private static final int SENSOR_COLOR_LOW = Color.parseColor("#FFFF3333");
    private static final int SENSOR_COLOR_NORMAL = Color.parseColor("#FF00BFFF");
    private static final int SENSOR_COLOR_HIGH = Color.parseColor("#FFFFBB33");

    private static final int PRIMARY_TEXT_COLOR_LOW = SENSOR_COLOR_LOW;
    private static final int PRIMARY_TEXT_COLOR_NORMAL = Color.WHITE;
    private static final int PRIMARY_TEXT_COLOR_HIGH = SENSOR_COLOR_HIGH;

    private static final int PRIMARY_TEXT_COLOR_STALE = Color.MAGENTA;

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void render(WatchFace watchFace) {
        if (watchFace.getDataPoints() == null) {
            Log.w(TAG, "No data points to render");
            return;
        }

        List<DataPoint> dataPoints = watchFace.getDataPoints();
        Settings settings = watchFace.getSettings();
        Canvas canvas = watchFace.getCanvas();
        ZonedDateTime now = watchFace.getNow();

        Rect rect = new Rect(
                (int) (watchFace.getBounds().width() * 0.1),
                (int) (watchFace.getBounds().height() * 0.35),
                (int) (watchFace.getBounds().width() * 0.9),
                (int) (watchFace.getBounds().height() * 0.8));

        Function<Instant, Integer> toX = instant -> {
            long t = instant.toEpochMilli();
            long maxT = watchFace.getNow().toInstant().toEpochMilli();
            long minT = maxT - watchFace.getSettings().getWatchFaceTimeWindow().toMillis();
            int minX = rect.left;
            int maxX = rect.right;
            return Math.toIntExact(minX + (maxX - minX) * (t - minT) / (maxT - minT));
        };

        DoubleSummaryStatistics glucoseStatistics = Stream.of(
                        dataPoints.stream()
                                .map(DataPoint::getSensorGlucose)
                                .filter(Objects::nonNull)
                                .map(sg -> sg.getMgdl(settings.isUseCalibrations())),
                        dataPoints.stream()
                                .map(DataPoint::getManualGlucose)
                                .filter(Objects::nonNull)
                                .map(DataPoint.ManualGlucose::getMgdl),
                        Stream.of(settings.lowThreshold, settings.highThreshold))
                .flatMap(Function.identity())
                .collect(Collectors.summarizingDouble(mgdl -> mgdl));

        Function<Double, Integer> toY = mgdl -> {
            double v = mgdl;
            double minV = glucoseStatistics.getMin();
            double maxV = glucoseStatistics.getMax();
            int minY = rect.bottom;
            int maxY = rect.top;
            return (int) (minY + (maxY - minY) * (v - minV) / (maxV - minV));
        };

        Function<DataPoint, Optional<ColorPoint>> sensorGlucoseToPoint = point ->
                Optional.ofNullable(point.getSensorGlucose())
                        .map(glucose -> {
                            double mgdl = glucose.getMgdl(settings.isUseCalibrations());
                            return new ColorPoint(
                                    toX.apply(point.getTimestamp()),
                                    toY.apply(mgdl),
                                    pickSensorGlucoseColor(settings, mgdl));
                        });

        renderTimeLines(canvas, rect, settings, now, toX);
        renderThresholdLines(canvas, rect, settings, toY);
        renderSensorGlucose(canvas, rect, settings, dataPoints, sensorGlucoseToPoint);
        renderText(canvas, rect, settings, dataPoints, now);
    }

    private void renderTimeLines(
            Canvas canvas,
            Rect rect,
            Settings settings,
            ZonedDateTime now,
            Function<Instant, Integer> toX)
    {
        Duration timeWindow = settings.getWatchFaceTimeWindow();
        ZonedDateTime from = now.minus(timeWindow);

        Paint linePaint = new Paint();
        linePaint.setColor(GRID_COLOR);
        Paint textPaint = new Paint();
        float textSize = rect.height() / 10f;
        textPaint.setColor(SECONDARY_TEXT_COLOR);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);

        int minutesPerLine;
        if (timeWindow.compareTo(Duration.ofMinutes(60)) > 0) minutesPerLine = 60;
        else if (timeWindow.compareTo(Duration.ofMinutes(30)) > 0) minutesPerLine = 30;
        else minutesPerLine = 15;

        for (ZonedDateTime t = DateTimeUtils.toStartOfNMinutes(from, minutesPerLine);
             t.isBefore(now); t = t.plus(Duration.ofMinutes(minutesPerLine))) {
            int x = toX.apply(t.toInstant());

            if (x >= rect.left && x < rect.right) {
                int y1 = rect.bottom;
                int y2 = rect.top;
                int y3 = (int) (rect.bottom + textSize * 1.2);
                canvas.drawLine(x, y1, x, y2, linePaint);
                canvas.drawText(t.format(timeFormatter), x, y3, textPaint);
            }
        }
    }

    private void renderThresholdLines(Canvas canvas, Rect rect, Settings settings, Function<Double, Integer> toY) {
        int x1 = rect.left;
        int x2 = rect.right;

        Paint paint = new Paint();
        paint.setStrokeWidth(rect.height() / 100f);

        paint.setColor(SENSOR_COLOR_LOW);
        int yLow = toY.apply(settings.lowThreshold);
        canvas.drawLine(x1, yLow, x2, yLow, paint);

        paint.setColor(SENSOR_COLOR_HIGH);
        int yHigh = toY.apply(settings.highThreshold);
        canvas.drawLine(x1, yHigh, x2, yHigh, paint);
    }

    private void renderSensorGlucose(
            Canvas canvas,
            Rect rect,
            Settings settings,
            List<DataPoint> dataPoints,
            Function<DataPoint, Optional<ColorPoint>> toPoint)
    {
        Paint paint = new Paint();
        float r = rect.width() * 20f / settings.getWatchFaceTimeWindow().getSeconds();

        dataPoints.forEach(dataPoint -> toPoint
                .apply(dataPoint)
                .ifPresent(point -> {
                    if (point.x >= rect.left && point.x <= rect.right) {
                        paint.setColor(point.getColor());
                        canvas.drawCircle(point.x, point.y, r, paint);
                    }
                }));
    }

    private void renderText(
            Canvas canvas,
            Rect rect,
            Settings settings,
            List<DataPoint> dataPoints,
            ZonedDateTime now)
    {
        Paint paint = new Paint();
        Optional<DataPoint> lastSensorPoint = dataPoints.stream()
                .filter(point -> point.getSensorGlucose() != null)
                .max(Comparator.comparing(DataPoint::getTimestamp))
                .filter(point -> Duration.between(point.getTimestamp(), now).compareTo(STALE_INTERVAL) < 0);

        String text = lastSensorPoint
                .map(point -> point.sensorGlucose)
                .map(glucose -> settings.getUnit().format(glucose.getMgdl(settings.isUseCalibrations())))
                .orElse("");

        float textHeight = rect.height() / 2.5f;
        float strokeWidth = textHeight / 15f;
        float textX = rect.left;
        float textY = rect.centerY();

        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(textHeight);

        paint.setColor(OUTLINE_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        canvas.drawText(text, textX, textY - (paint.descent() + paint.ascent()) / 2, paint);

        lastSensorPoint.map(point -> point.sensorGlucose).ifPresentOrElse(
                glucose -> paint.setColor(
                        pickSensorGlucoseTextColor(settings, glucose.getMgdl(settings.isUseCalibrations()))),
                () -> paint.setColor(PRIMARY_TEXT_COLOR_STALE));

        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(text, textX, textY - (paint.descent() + paint.ascent()) / 2, paint);
    }

    private int pickSensorGlucoseColor(Settings settings, double mgdl) {
        if (mgdl < settings.getLowThreshold()) return SENSOR_COLOR_LOW;
        if (mgdl > settings.getHighThreshold()) return SENSOR_COLOR_HIGH;
        return SENSOR_COLOR_NORMAL;
    }

    private int pickSensorGlucoseTextColor(Settings settings, double mgdl) {
        if (mgdl < settings.getLowThreshold()) return PRIMARY_TEXT_COLOR_LOW;
        if (mgdl > settings.getHighThreshold()) return PRIMARY_TEXT_COLOR_HIGH;
        return PRIMARY_TEXT_COLOR_NORMAL;
    }

    @Data
    private static class ColorPoint {
        private final int x;
        private final int y;
        private final int color;
    }
}
