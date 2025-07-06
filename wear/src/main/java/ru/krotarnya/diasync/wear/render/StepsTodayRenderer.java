package ru.krotarnya.diasync.wear.render;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.Optional;

import ru.krotarnya.diasync.wear.model.WatchFaceData;

final class StepsTodayRenderer implements ComponentRenderer {
    private static final int COLOR = Color.WHITE;
    private final Paint paint;

    public StepsTodayRenderer() {
        this.paint = new Paint();
        paint.setFakeBoldText(true);
        paint.setColor(COLOR);
        paint.setTextAlign(Paint.Align.RIGHT);
    }

    @Override
    public void render(WatchFaceData watchFaceData) {
        Optional.ofNullable(watchFaceData.getStepsToday())
                .ifPresent(stepsToday -> {
                    paint.setTextSize(watchFaceData.getBounds().height() / 15f);

                    watchFaceData.getCanvas().drawText(
                            String.valueOf(stepsToday),
                            (int) (watchFaceData.getBounds().width() * 0.9),
                            watchFaceData.getBounds().height() * 0.28f - (paint.descent() + paint.ascent()) / 2,
                            paint);
                });
    }
}
