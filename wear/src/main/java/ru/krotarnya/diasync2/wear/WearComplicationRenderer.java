package ru.krotarnya.diasync2.wear;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

final class WearComplicationRenderer {
    static final int BACKGROUND_COLOR = 0xFF000000;
    static final int NORMAL_COLOR = 0xFFFFFFFF;
    static final int HIGH_COLOR = 0xFFFFBB33;
    static final int LOW_COLOR = 0xFFC30909;
    static final int ERROR_COLOR = 0xFFFF0000;
    private static final int GRID_COLOR = 0xFF505050;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final WearGraphLayoutCalculator layoutCalculator;

    WearComplicationRenderer() {
        this(new WearGraphLayoutCalculator());
    }

    WearComplicationRenderer(WearGraphLayoutCalculator layoutCalculator) {
        this.layoutCalculator = layoutCalculator;
    }

    Bitmap render(WearComplicationState state, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(BACKGROUND_COLOR);
        Canvas canvas = new Canvas(bitmap);
        if (!state.hasGlucose()) {
            drawCenteredText(canvas, state.message(), width / 2f, height / 2f,
                    Math.max(22f, height * 0.14f), ERROR_COLOR);
            return bitmap;
        }

        WearGraphLayout layout = layoutCalculator.calculate(state, width, height);
        drawZones(canvas, state, layout);
        drawTimeLines(canvas, state, layout);
        drawThresholdLines(canvas, state, layout);
        drawPoints(canvas, state, layout);
        drawCurrentValue(canvas, state, layout);
        if (state.kind() == WearComplicationState.Kind.STALE) {
            drawCenteredText(canvas, state.message(), width / 2f, height * 0.94f,
                    Math.max(18f, height * 0.11f), ERROR_COLOR);
        }
        return bitmap;
    }

    private void drawZones(Canvas canvas, WearComplicationState state, WearGraphLayout layout) {
        if (!state.graphZones()) {
            return;
        }
        Paint paint = new Paint();
        paint.setColor(Color.argb(42, 195, 9, 9));
        canvas.drawRect(
                layout.bounds().left(),
                layout.lowY(),
                layout.bounds().right(),
                layout.bounds().bottom(),
                paint);
        paint.setColor(Color.argb(38, 255, 187, 51));
        canvas.drawRect(
                layout.bounds().left(),
                layout.bounds().top(),
                layout.bounds().right(),
                layout.highY(),
                paint);
    }

    private void drawTimeLines(
            Canvas canvas,
            WearComplicationState state,
            WearGraphLayout layout
    ) {
        int intervalMinutes = state.graphWindowMinutes() > 60
                ? 60
                : state.graphWindowMinutes() > 30 ? 30 : 15;
        ZonedDateTime end = state.renderedAt().atZone(ZoneId.systemDefault());
        ZonedDateTime tick = end.minusMinutes(state.graphWindowMinutes())
                .withSecond(0)
                .withNano(0);
        int remainder = tick.getMinute() % intervalMinutes;
        if (remainder != 0) {
            tick = tick.plusMinutes(intervalMinutes - remainder);
        }
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(GRID_COLOR);
        line.setStrokeWidth(1f);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.GRAY);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(Math.max(9f, layout.bounds().height() * 0.08f));
        long windowMillis = Duration.ofMinutes(state.graphWindowMinutes()).toMillis();
        long startMillis = state.renderedAt().toEpochMilli() - windowMillis;
        while (tick.toInstant().isBefore(state.renderedAt())) {
            float x = layout.bounds().left() + layout.bounds().width()
                    * (tick.toInstant().toEpochMilli() - startMillis) / windowMillis;
            if (x >= layout.bounds().left() && x <= layout.bounds().right()) {
                canvas.drawLine(x, layout.bounds().top(), x, layout.bounds().bottom(), line);
                canvas.drawText(tick.format(TIME_FORMAT), x,
                        layout.bounds().bottom() + text.getTextSize(), text);
            }
            tick = tick.plusMinutes(intervalMinutes);
        }
    }

    private void drawThresholdLines(
            Canvas canvas,
            WearComplicationState state,
            WearGraphLayout layout
    ) {
        if (!state.graphLines()) {
            return;
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(Math.max(1f, layout.bounds().height() / 80f));
        paint.setColor(LOW_COLOR);
        canvas.drawLine(layout.bounds().left(), layout.lowY(),
                layout.bounds().right(), layout.lowY(), paint);
        paint.setColor(HIGH_COLOR);
        canvas.drawLine(layout.bounds().left(), layout.highY(),
                layout.bounds().right(), layout.highY(), paint);
    }

    private void drawPoints(
            Canvas canvas,
            WearComplicationState state,
            WearGraphLayout layout
    ) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (WearGraphLayout.Point point : layout.points()) {
            paint.setColor(rangeColor(point.displayMgDl(), state.lowMgDl(), state.highMgDl()));
            canvas.drawCircle(point.x(), point.y(), layout.pointRadius(), paint);
        }
    }

    private void drawCurrentValue(
            Canvas canvas,
            WearComplicationState state,
            WearGraphLayout layout
    ) {
        String text = state.valueWithTrend();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(Math.min(layout.bounds().height() * 0.42f,
                layout.bounds().width() / Math.max(3.8f, text.length() * 0.58f)));
        float x = layout.bounds().left() + 4f;
        float y = layout.bounds().centerY() - (paint.descent() + paint.ascent()) / 2f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(3f, paint.getTextSize() / 12f));
        paint.setColor(BACKGROUND_COLOR);
        canvas.drawText(text, x, y, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(rangeColor(
                state.latestDisplayMgDl(), state.lowMgDl(), state.highMgDl()));
        canvas.drawText(text, x, y, paint);
    }

    @SuppressWarnings("SameParameterValue")
    private void drawCenteredText(
            Canvas canvas,
            String text,
            float centerX,
            float centerY,
            float size,
            int color
    ) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size);
        paint.setColor(color);
        canvas.drawText(text, centerX, centerY - (paint.descent() + paint.ascent()) / 2f, paint);
    }

    static int rangeColor(double value, double low, double high) {
        if (value <= low) {
            return LOW_COLOR;
        }
        if (value >= high) {
            return HIGH_COLOR;
        }
        return NORMAL_COLOR;
    }
}
