package ru.krotarnya.diasync2.wear;

import java.util.Optional;
import ru.krotarnya.diasync2.common.GlucoseValue;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

final class WearComplicationStateMapper {
    private static final int SHORT_TEXT_MAX_LENGTH = 7;

    WearComplicationState map(Optional<WearSnapshot> snapshot) {
        if (snapshot.isEmpty() || snapshot.orElseThrow().points().isEmpty()) {
            return noData();
        }
        WearSnapshot state = snapshot.orElseThrow();
        WearGlucosePoint latest = state.points().get(0);
        double displayMgDl = latest.displayMgDl(state.display().useCalibration());
        if (!Double.isFinite(displayMgDl) || displayMgDl < 0.0) {
            return noData();
        }
        String value = new GlucoseValue(displayMgDl).format(state.display().unit());
        String trend = state.display().trendArrow() ? state.display().trend() : "";
        String text = trend.isEmpty() ? value : value + " " + trend;
        if (text.length() > SHORT_TEXT_MAX_LENGTH) {
            text = value;
        }
        if (text.length() > SHORT_TEXT_MAX_LENGTH) {
            return noData();
        }
        return new WearComplicationState(
                text,
                state.display().unit().symbol(),
                "Glucose " + value + " " + state.display().unit().symbol()
                        + (trend.isEmpty() ? "" : ", trend " + trend));
    }

    private WearComplicationState noData() {
        return new WearComplicationState("NO DATA", "Diasync", "No glucose data");
    }
}
