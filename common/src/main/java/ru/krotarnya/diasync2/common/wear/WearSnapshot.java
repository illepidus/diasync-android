package ru.krotarnya.diasync2.common.wear;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record WearSnapshot(
        int protocolVersion,
        Instant generatedAt,
        List<WearGlucosePoint> points,
        WearDisplayPolicy display,
        WearAlertPolicy alerts,
        WearAlertEvent alertEvent
) {
    public static final int PROTOCOL_VERSION = 1;
    public static final Duration MAX_GRAPH_WINDOW = Duration.ofHours(3);
    public static final Duration GRAPH_WINDOW_MARGIN = Duration.ofMinutes(10);
    public static final int MAX_POINTS = 256;
    public static final int MAX_PAYLOAD_BYTES = 64 * 1024;
    public static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(1);

    public WearSnapshot {
        Objects.requireNonNull(generatedAt);
        Objects.requireNonNull(points);
        Objects.requireNonNull(display);
        Objects.requireNonNull(alerts);
        points = List.copyOf(points);
        if (protocolVersion != PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Protocol version is unsupported");
        }
        if (points.size() > MAX_POINTS) {
            throw new IllegalArgumentException("Snapshot contains too many points");
        }
        Instant oldestAllowed = generatedAt.minus(MAX_GRAPH_WINDOW).minus(GRAPH_WINDOW_MARGIN);
        Instant newestAllowed = generatedAt.plus(MAX_FUTURE_SKEW);
        Instant previous = null;
        for (WearGlucosePoint point : points) {
            Objects.requireNonNull(point);
            if (point.timestamp().isBefore(oldestAllowed)
                    || point.timestamp().isAfter(newestAllowed)) {
                throw new IllegalArgumentException("Point is outside snapshot window");
            }
            if (previous != null && point.timestamp().isAfter(previous)) {
                throw new IllegalArgumentException("Points must be newest first");
            }
            previous = point.timestamp();
        }
    }
}
