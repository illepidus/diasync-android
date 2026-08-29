package ru.krotarnya.diasync2.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class WidgetTapRouterTest {
    @Test public void tapAtBoundaryOpensAlerts() {
        WidgetTapRouter router = new WidgetTapRouter();
        assertEquals(WidgetTapRouter.Result.WAIT_FOR_SECOND_TAP, router.tap(1000));
        assertEquals(WidgetTapRouter.Result.OPEN_ALERTS,
                router.tap(1000 + WidgetTapRouter.DOUBLE_TAP_WINDOW_MILLIS));
        assertFalse(router.consumeSingleTap(1000));
    }

    @Test public void tapAfterBoundaryStartsNewSingleTap() {
        WidgetTapRouter router = new WidgetTapRouter();
        router.tap(1000);
        long second = 1001 + WidgetTapRouter.DOUBLE_TAP_WINDOW_MILLIS;
        assertEquals(WidgetTapRouter.Result.WAIT_FOR_SECOND_TAP, router.tap(second));
        assertTrue(router.consumeSingleTap(second));
    }
}
