package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Wearable;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearAlertPolicy;
import ru.krotarnya.diasync2.common.wear.WearDisplayPolicy;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;
import ru.krotarnya.diasync2.common.wear.WearSnapshotCodec;

@RunWith(AndroidJUnit4.class)
public class WearDataLayerDeviceTest {
    @Test
    public void publishesUrgentStateDataItem() throws Exception {
        Instant now = Instant.now();
        WearSnapshot snapshot = new WearSnapshot(
                WearSnapshot.PROTOCOL_VERSION,
                now,
                List.of(new WearGlucosePoint(now.minusSeconds(30), 123.0, null, null)),
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
        Context context = ApplicationProvider.getApplicationContext();
        assertFalse(Tasks.await(
                Wearable.getNodeClient(context).getConnectedNodes(),
                10,
                TimeUnit.SECONDS).isEmpty());
        DataItem item = Tasks.await(
                Wearable.getDataClient(context)
                        .putDataItem(new WearDataRequestFactory().create(
                                new WearSnapshotCodec().encode(snapshot))),
                10,
                TimeUnit.SECONDS);

        assertEquals(WearDataRequestFactory.STATE_PATH, item.getUri().getPath());
    }
}
