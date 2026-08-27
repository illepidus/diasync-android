package ru.krotarnya.diasync2.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

final class TrendIconRenderer {
    private static final float VIEWBOX = 24.0f;

    Bitmap render(String trend, int sizePx, int color) {
        int boundedSize = Math.max(1, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(boundedSize, boundedSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(boundedSize / VIEWBOX, boundedSize / VIEWBOX);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.0f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        switch (trend) {
            case "⇊" -> {
                drawDown(canvas, paint, 8.0f);
                drawDown(canvas, paint, 16.0f);
            }
            case "↓" -> drawDown(canvas, paint, 12.0f);
            case "↘" -> drawDownRight(canvas, paint);
            case "→" -> drawRight(canvas, paint);
            case "↗" -> drawUpRight(canvas, paint);
            case "↑" -> drawUp(canvas, paint, 12.0f);
            case "⇈" -> {
                drawUp(canvas, paint, 8.0f);
                drawUp(canvas, paint, 16.0f);
            }
            default -> canvas.drawLine(6.0f, 12.0f, 18.0f, 12.0f, paint);
        }
        return bitmap;
    }

    private void drawRight(Canvas canvas, Paint paint) {
        canvas.drawLine(4.0f, 12.0f, 20.0f, 12.0f, paint);
        canvas.drawLine(14.0f, 6.0f, 20.0f, 12.0f, paint);
        canvas.drawLine(14.0f, 18.0f, 20.0f, 12.0f, paint);
    }

    private void drawUp(Canvas canvas, Paint paint, float x) {
        canvas.drawLine(x, 20.0f, x, 4.0f, paint);
        canvas.drawLine(x - 4.0f, 9.0f, x, 4.0f, paint);
        canvas.drawLine(x + 4.0f, 9.0f, x, 4.0f, paint);
    }

    private void drawDown(Canvas canvas, Paint paint, float x) {
        canvas.drawLine(x, 4.0f, x, 20.0f, paint);
        canvas.drawLine(x - 4.0f, 15.0f, x, 20.0f, paint);
        canvas.drawLine(x + 4.0f, 15.0f, x, 20.0f, paint);
    }

    private void drawUpRight(Canvas canvas, Paint paint) {
        canvas.drawLine(4.0f, 20.0f, 20.0f, 4.0f, paint);
        canvas.drawLine(12.0f, 4.0f, 20.0f, 4.0f, paint);
        canvas.drawLine(20.0f, 4.0f, 20.0f, 12.0f, paint);
    }

    private void drawDownRight(Canvas canvas, Paint paint) {
        canvas.drawLine(4.0f, 4.0f, 20.0f, 20.0f, paint);
        canvas.drawLine(12.0f, 20.0f, 20.0f, 20.0f, paint);
        canvas.drawLine(20.0f, 12.0f, 20.0f, 20.0f, paint);
    }
}
