package ru.krotarnya.diasync.wear.render;

import android.graphics.Canvas;
import android.graphics.Rect;

import java.time.ZonedDateTime;

import ru.krotarnya.diasync.common.model.WidgetData;

public interface ComponentRenderer {
    void render(Canvas canvas, Rect bounds, ZonedDateTime zonedDateTime, WidgetData widgetData);
}
