package ru.krotarnya.diasync2.wear;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

final class WearDiagnosticPresenter {
    private final Clock clock;
    WearDiagnosticPresenter(Clock clock) { this.clock = clock; }

    WearDiagnosticState present(Optional<WearSnapshot> value, Instant receivedAt,
            String lastError, WearDataPhase watchdogPhase) {
        if (value.isEmpty()) {
            return new WearDiagnosticState(lastError == null ? "NO SNAPSHOT" : "REJECTED PAYLOAD",
                    "Waiting for a valid phone snapshot",
                    "Protocol: —\nReceived: never",
                    "Time window: —\nPoints: 0\nUnit: —\nCalibration: —"
                            + "\nZones: —\nLines: —\nTrend: —",
                    "Watchdog: " + watchdogPhase.name(),
                    lastError == null ? "" : lastError);
        }
        WearSnapshot snapshot = value.get();
        Instant now = clock.instant();
        String snapshotDetails = "Protocol: " + snapshot.protocolVersion()
                + "\nGenerated: " + pastAge(now, snapshot.generatedAt())
                + "\nReceived: " + pastAge(now, receivedAt);
        String display = "Time window: " + snapshot.display().graphWindowMinutes() + "m"
                + "\nPoints: " + snapshot.points().size()
                + "\nUnit: " + snapshot.display().unit().symbol()
                + "\nCalibration: " + onOff(snapshot.display().useCalibration())
                + "\nZones: " + onOff(snapshot.display().graphZones())
                + "\nLines: " + onOff(snapshot.display().graphLines())
                + "\nTrend: " + onOff(snapshot.display().trendArrow());
        String policy = "Low: " + onOff(snapshot.alerts().lowEnabled())
                + "\nHigh: " + onOff(snapshot.alerts().highEnabled())
                + "\nNo data: " + onOff(snapshot.alerts().noDataEnabled())
                + "\nSnooze: " + (snapshot.alerts().snoozedUntil().isAfter(now)
                ? age(snapshot.alerts().snoozedUntil(), now) + " left" : "off")
                + "\nWatchdog: " + watchdogPhase.name();
        if (snapshot.points().isEmpty()) {
            return new WearDiagnosticState(lastError == null ? "NO DATA" : "REJECTED PAYLOAD",
                    "No glucose points", snapshotDetails, display, policy,
                    lastError == null ? "" : lastError);
        }
        WearGlucosePoint latest = snapshot.points().get(0);
        GlucoseUnit unit = snapshot.display().unit();
        Duration dataAge = duration(now, latest.timestamp());
        String phase = dataAge.compareTo(Duration.ofMinutes(5)) >= 0 ? "NO DATA"
                : dataAge.compareTo(Duration.ofSeconds(90)) >= 0 ? "STALE" : "VALID";
        String trend = snapshot.display().trend().isBlank() ? "—" : snapshot.display().trend();
        String formatted = unit.formatFromMgDl(latest.displayMgDl(snapshot.display().useCalibration()));
        return new WearDiagnosticState(lastError == null ? phase : "REJECTED PAYLOAD",
                formatted + " " + unit.symbol() + " " + trend
                + "\nMeasured " + pastAge(now, latest.timestamp()),
                snapshotDetails,
                display,
                policy,
                lastError == null ? "" : lastError);
    }

    private String onOff(boolean enabled) { return enabled ? "on" : "off"; }
    private String age(Instant later, Instant earlier) {
        long seconds = duration(later, earlier).getSeconds();
        return seconds < 60 ? seconds + "s" : (seconds / 60) + "m";
    }

    private String pastAge(Instant now, Instant timestamp) {
        return timestamp == null ? "never" : age(now, timestamp) + " ago";
    }

    private Duration duration(Instant later, Instant earlier) {
        Duration value = Duration.between(earlier, later);
        return value.isNegative() ? Duration.ZERO : value;
    }
}
