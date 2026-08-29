package ru.krotarnya.diasync2.widget;

public final class WidgetTapRouter {
    public static final long DOUBLE_TAP_WINDOW_MILLIS = 350L;

    public enum Result { WAIT_FOR_SECOND_TAP, OPEN_ALERTS }

    private long firstTapAt = Long.MIN_VALUE;

    public Result tap(long elapsedRealtimeMillis) {
        if (firstTapAt != Long.MIN_VALUE
                && elapsedRealtimeMillis - firstTapAt <= DOUBLE_TAP_WINDOW_MILLIS) {
            firstTapAt = Long.MIN_VALUE;
            return Result.OPEN_ALERTS;
        }
        firstTapAt = elapsedRealtimeMillis;
        return Result.WAIT_FOR_SECOND_TAP;
    }

    public boolean consumeSingleTap(long expectedTapAt) {
        if (firstTapAt != expectedTapAt) {
            return false;
        }
        firstTapAt = Long.MIN_VALUE;
        return true;
    }
}
