package ru.krotarnya.diasync2.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import ru.krotarnya.diasync2.settings.GraphWindow;

public class WidgetGraphLayoutCalculatorTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private final WidgetGraphLayoutCalculator calculator = new WidgetGraphLayoutCalculator();

    @Test
    public void mapsWindowEdgesInsideBitmapBounds() {
        WidgetGraphLayout layout = calculate(List.of(
                sample(NOW.minus(Duration.ofMinutes(31)), 70.0),
                sample(NOW.plus(Duration.ofMinutes(1)), 180.0)));

        assertEquals(2, layout.points().size());
        assertEquals(layout.pointRadius(), layout.points().get(0).x(), 0.01f);
        assertEquals(layout.width() - layout.pointRadius(), layout.points().get(1).x(), 0.01f);
        for (WidgetGraphLayout.Point point : layout.points()) {
            assertTrue(point.x() >= layout.pointRadius());
            assertTrue(point.x() <= layout.width() - layout.pointRadius());
            assertTrue(point.y() >= layout.pointRadius());
            assertTrue(point.y() <= layout.height() - layout.pointRadius());
        }
    }

    @Test
    public void keepsEdgePointsInsideMinimumAndMaximumBitmapSizes() {
        for (int[] size : List.of(
                new int[]{40, 40},
                new int[]{40, 160},
                new int[]{160, 40},
                new int[]{1024, 1024})) {
            WidgetGraphLayout layout = calculator.calculate(
                    List.of(
                            sample(NOW.minus(Duration.ofMinutes(31)), 70.0),
                            sample(NOW.plus(Duration.ofMinutes(1)), 180.0)),
                    NOW,
                    Duration.ofMinutes(30),
                    70.0,
                    180.0,
                    size[0],
                    size[1]);

            for (WidgetGraphLayout.Point point : layout.points()) {
                assertTrue(point.x() - layout.pointRadius() >= 0.0f);
                assertTrue(point.x() + layout.pointRadius() <= layout.width());
                assertTrue(point.y() - layout.pointRadius() >= 0.0f);
                assertTrue(point.y() + layout.pointRadius() <= layout.height());
            }
        }
    }

    @Test
    public void threeHourWindowIncludesPointsExcludedByShorterWindows() {
        WidgetGraphSample twoHoursOld = sample(NOW.minus(Duration.ofHours(2)), 100.0);

        WidgetGraphLayout oneHour = calculator.calculate(
                List.of(twoHoursOld),
                NOW,
                GraphWindow.ONE_HOUR.duration(),
                70.0,
                180.0,
                200,
                100);
        WidgetGraphLayout threeHours = calculator.calculate(
                List.of(twoHoursOld),
                NOW,
                GraphWindow.THREE_HOURS.duration(),
                70.0,
                180.0,
                200,
                100);

        assertTrue(oneHour.points().isEmpty());
        assertEquals(1, threeHours.points().size());
    }

    @Test
    public void pointRadiusShrinksAsTheTimeWindowGrows() {
        WidgetGraphLayout thirtyMinutes = calculateRadius(Duration.ofMinutes(30), 400, 400);
        WidgetGraphLayout oneHour = calculateRadius(Duration.ofHours(1), 400, 400);
        WidgetGraphLayout threeHours = calculateRadius(Duration.ofHours(3), 400, 400);

        assertTrue(thirtyMinutes.pointRadius() > oneHour.pointRadius());
        assertTrue(oneHour.pointRadius() > threeHours.pointRadius());
    }

    @Test
    public void pointRadiusScalesWithWidgetWidth() {
        WidgetGraphLayout narrow = calculateRadius(Duration.ofHours(1), 200, 1000);
        WidgetGraphLayout wide = calculateRadius(Duration.ofHours(1), 400, 1000);

        assertEquals(narrow.pointRadius() * 2.0f, wide.pointRadius(), 0.001f);
    }

    @Test
    public void excludesPointsOutsideTimeBounds() {
        WidgetGraphLayout layout = calculate(List.of(
                sample(NOW.minus(Duration.ofMinutes(31)).minusMillis(1), 100.0),
                sample(NOW.minus(Duration.ofMinutes(15)), 100.0),
                sample(NOW.plus(Duration.ofMinutes(1)).plusMillis(1), 100.0)));

        assertEquals(1, layout.points().size());
    }

    @Test
    public void autoScaleIncludesDataThresholdsAndMargin() {
        WidgetGraphLayout layout = calculate(List.of(sample(NOW, 250.0)));

        WidgetGraphLayout.Point point = layout.points().get(0);
        assertTrue(point.y() > 0.0f);
        assertTrue(layout.highY() > point.y());
        assertTrue(layout.lowY() > layout.highY());
        assertTrue(layout.lowY() < layout.height());
    }

    @Test
    public void identicalThresholdAndValuesStillProduceFiniteCoordinates() {
        WidgetGraphLayout layout = calculator.calculate(
                List.of(sample(NOW, 100.0)),
                NOW,
                Duration.ofMinutes(30),
                100.0,
                100.0,
                1,
                1);

        assertTrue(Float.isFinite(layout.points().get(0).x()));
        assertTrue(Float.isFinite(layout.points().get(0).y()));
        assertTrue(Double.isFinite(layout.lowY()));
        assertTrue(Double.isFinite(layout.highY()));
    }

    @Test
    public void classifiesPointsUsingConfiguredThresholds() {
        WidgetGraphLayout layout = calculator.calculate(
                List.of(
                        sample(NOW.minusSeconds(2), 80.0),
                        sample(NOW.minusSeconds(1), 120.0),
                        sample(NOW, 160.0)),
                NOW,
                Duration.ofMinutes(30),
                80.0,
                160.0,
                200,
                100);

        assertEquals(WidgetState.Range.LOW, layout.points().get(0).range());
        assertEquals(WidgetState.Range.NORMAL, layout.points().get(1).range());
        assertEquals(WidgetState.Range.HIGH, layout.points().get(2).range());
    }

    private WidgetGraphLayout calculate(List<WidgetGraphSample> samples) {
        return calculator.calculate(
                samples,
                NOW,
                Duration.ofMinutes(30),
                70.0,
                180.0,
                200,
                100);
    }

    private WidgetGraphLayout calculateRadius(Duration window, int width, int height) {
        return calculator.calculate(
                List.of(sample(NOW, 100.0)),
                NOW,
                window,
                70.0,
                180.0,
                width,
                height);
    }

    private WidgetGraphSample sample(Instant timestamp, double mgDl) {
        return new WidgetGraphSample(timestamp, mgDl);
    }
}
