package ru.krotarnya.diasync2.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import ru.krotarnya.diasync2.MainActivity;
import ru.krotarnya.diasync2.R;

final class WidgetRemoteViewsFactory {
    private final TrendIconRenderer trendIconRenderer = new TrendIconRenderer();

    RemoteViews create(
            Context context,
            WidgetState state,
            Bitmap graphBitmap,
            WidgetBitmapSize size
    ) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_latest_value);
        views.setTextViewText(R.id.widget_value, state.value());
        views.setTextViewText(R.id.widget_message, state.message());
        views.setViewVisibility(R.id.widget_value, state.valueVisible() ? View.VISIBLE : View.INVISIBLE);
        views.setTextViewTextSize(
                R.id.widget_value,
                TypedValue.COMPLEX_UNIT_SP,
                size.valueTextSp());
        views.setTextViewTextSize(
                R.id.widget_message,
                TypedValue.COMPLEX_UNIT_SP,
                size.messageTextSp());
        int trendSizePx = Math.max(1, Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                size.trendIconDp(),
                context.getResources().getDisplayMetrics())));
        views.setImageViewBitmap(
                R.id.widget_trend,
                trendIconRenderer.render(
                        state.trend(),
                        trendSizePx,
                        context.getColor(R.color.widget_secondary)));
        views.setViewVisibility(
                R.id.widget_trend,
                state.trendVisible() ? View.VISIBLE : View.GONE);
        views.setContentDescription(R.id.widget_trend, state.trend());
        views.setInt(
                R.id.widget_value,
                "setPaintFlags",
                Paint.ANTI_ALIAS_FLAG
                        | Paint.SUBPIXEL_TEXT_FLAG
                        | (state.strikeThrough() ? Paint.STRIKE_THRU_TEXT_FLAG : 0));
        views.setTextColor(R.id.widget_value, context.getColor(colorResource(state.range())));
        if (graphBitmap == null) {
            views.setImageViewResource(R.id.widget_graph, android.R.color.transparent);
        } else {
            views.setImageViewBitmap(R.id.widget_graph, graphBitmap);
        }
        views.setOnClickPendingIntent(R.id.widget_root, openActivityIntent(context));
        return views;
    }

    private int colorResource(WidgetState.Range range) {
        return switch (range) {
            case LOW -> R.color.widget_low;
            case NORMAL -> R.color.widget_normal;
            case HIGH -> R.color.widget_high;
            case ERROR -> R.color.widget_error;
        };
    }

    private PendingIntent openActivityIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
