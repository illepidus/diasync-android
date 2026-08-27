package ru.krotarnya.diasync2.presentation;

import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Test;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.GlucoseValue;
import ru.krotarnya.diasync2.common.SensorPoint;
import ru.krotarnya.diasync2.data.BootstrapResult;

public class StatusPresenterTest {
    private final StatusPresenter presenter = new StatusPresenter(Clock.fixed(
            Instant.parse("2026-08-27T12:05:30Z"),
            ZoneOffset.UTC));

    @Test
    public void exposesConfigurationLoadingNoDataAndErrorStates() {
        assertEquals(
                StatusState.Kind.CONFIGURATION_MISSING,
                presenter.configurationMissing().kind());
        assertEquals(StatusState.Kind.LOADING, presenter.loading().kind());
        assertEquals(
                StatusState.Kind.NO_DATA,
                presenter.bootstrap(
                        BootstrapResult.noData(),
                        GlucoseUnit.MMOL_L,
                        true).kind());
        assertEquals(
                StatusState.Kind.CONNECTION_ERROR,
                presenter.bootstrap(
                        BootstrapResult.error(BootstrapResult.Kind.CONNECTION_ERROR),
                        GlucoseUnit.MMOL_L,
                        true).kind());
        assertEquals(
                StatusState.Kind.HTTP_ERROR,
                presenter.bootstrap(
                        BootstrapResult.error(BootstrapResult.Kind.HTTP_ERROR),
                        GlucoseUnit.MMOL_L,
                        true).kind());
        assertEquals(
                StatusState.Kind.PARSE_ERROR,
                presenter.bootstrap(
                        BootstrapResult.error(BootstrapResult.Kind.PARSE_ERROR),
                        GlucoseUnit.MMOL_L,
                        true).kind());
        assertEquals(
                StatusState.Kind.INVALID_DATA,
                presenter.bootstrap(
                        BootstrapResult.error(BootstrapResult.Kind.INVALID_DATA),
                        GlucoseUnit.MMOL_L,
                        true).kind());
        assertEquals(
                StatusState.Kind.STORAGE_ERROR,
                presenter.bootstrap(
                        BootstrapResult.error(BootstrapResult.Kind.STORAGE_ERROR),
                        GlucoseUnit.MMOL_L,
                        true).kind());
    }

    @Test
    public void formatsLatestValueTimestampAndClockBasedAge() {
        SensorPoint sensor = new SensorPoint(
                Instant.parse("2026-08-27T12:00:00Z"),
                new GlucoseValue(180.0),
                "sensor",
                null);
        DataPoint point = new DataPoint(sensor.timestamp(), null, sensor, null, null, null);

        StatusState state = presenter.bootstrap(
                BootstrapResult.success(point),
                GlucoseUnit.MMOL_L,
                true);

        assertEquals(StatusState.Kind.LATEST_VALUE, state.kind());
        assertEquals("10.0", state.value());
        assertEquals("mmol/L", state.unit());
        assertEquals("2026-08-27T12:00:00Z", state.timestamp());
        assertEquals("5 min ago", state.age());
    }
}
