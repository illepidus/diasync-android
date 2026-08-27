package ru.krotarnya.diasync2.widget;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import ru.krotarnya.diasync2.settings.GraphWindow;

public final class WidgetState {
    public enum Range {
        LOW,
        NORMAL,
        HIGH,
        ERROR
    }

    private final String value;
    private final String trend;
    private final String message;
    private final Range range;
    private final boolean valueVisible;
    private final boolean strikeThrough;
    private final Instant generatedAt;
    private final List<WidgetGraphSample> graphSamples;
    private final GraphWindow graphWindow;
    private final double lowMgDl;
    private final double highMgDl;
    private final boolean graphZones;
    private final boolean graphLines;
    private final boolean trendVisible;
    private final boolean graphVisible;

    public WidgetState(
            String value,
            String trend,
            String message,
            Range range,
            boolean valueVisible,
            boolean strikeThrough,
            Instant generatedAt,
            List<WidgetGraphSample> graphSamples,
            GraphWindow graphWindow,
            double lowMgDl,
            double highMgDl,
            boolean graphZones,
            boolean graphLines,
            boolean trendVisible,
            boolean graphVisible
    ) {
        this.value = Objects.requireNonNull(value);
        this.trend = Objects.requireNonNull(trend);
        this.message = Objects.requireNonNull(message);
        this.range = Objects.requireNonNull(range);
        this.valueVisible = valueVisible;
        this.strikeThrough = strikeThrough;
        this.generatedAt = Objects.requireNonNull(generatedAt);
        this.graphSamples = List.copyOf(graphSamples);
        this.graphWindow = Objects.requireNonNull(graphWindow);
        this.lowMgDl = lowMgDl;
        this.highMgDl = highMgDl;
        this.graphZones = graphZones;
        this.graphLines = graphLines;
        this.trendVisible = trendVisible;
        this.graphVisible = graphVisible;
    }

    public String value() {
        return value;
    }

    public String trend() {
        return trend;
    }

    public String message() {
        return message;
    }

    public Range range() {
        return range;
    }

    public boolean valueVisible() {
        return valueVisible;
    }

    public boolean strikeThrough() {
        return strikeThrough;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<WidgetGraphSample> graphSamples() {
        return graphSamples;
    }

    public GraphWindow graphWindow() {
        return graphWindow;
    }

    public double lowMgDl() {
        return lowMgDl;
    }

    public double highMgDl() {
        return highMgDl;
    }

    public boolean graphZones() {
        return graphZones;
    }

    public boolean graphLines() {
        return graphLines;
    }

    public boolean trendVisible() {
        return trendVisible;
    }

    public boolean graphVisible() {
        return graphVisible;
    }
}
