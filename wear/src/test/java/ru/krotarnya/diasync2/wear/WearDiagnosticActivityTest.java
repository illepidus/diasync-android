package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import ru.krotarnya.diasync2.R;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;
import ru.krotarnya.diasync2.common.wear.WearSnapshotCodec;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WearDiagnosticActivityTest {
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

    @Before
    public void clearState() {
        RuntimeEnvironment.getApplication()
                .createDeviceProtectedStorageContext()
                .getSharedPreferences("wear_last_known_state", 0)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void openActivityRefreshesWhenSnapshotArrives() {
        WearDiagnosticActivity activity = Robolectric.buildActivity(WearDiagnosticActivity.class)
                .create()
                .start()
                .resume()
                .get();
        TextView value = activity.findViewById(R.id.wear_latest_value);
        assertEquals("NO DATA", value.getText().toString());
        LastKnownWearStateRepository.create(activity).replaceIfValid(
                new WearSnapshotCodec().encode(snapshot(126.0)));

        activity.sendBroadcast(new Intent(WearDiagnosticActivity.ACTION_STATE_UPDATED)
                .setPackage(activity.getPackageName()));
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("Latest: 7.0 mmol/L", value.getText().toString());
    }

    private WearSnapshot snapshot(double value) {
        return new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                NOW,
                List.of(new WearGlucosePoint(NOW.minusSeconds(30), value, null, null)),
                new WearDisplayPolicy(
                        GlucoseUnit.MMOL_L,
                        true,
                        70.0,
                        180.0,
                        30,
                        true,
                        false,
                        true,
                        "→"),
                new WearAlertPolicy(false, false, true, Instant.EPOCH),
                null);
    }
}
