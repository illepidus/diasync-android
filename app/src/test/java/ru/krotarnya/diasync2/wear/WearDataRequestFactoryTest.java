package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataRequest;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WearDataRequestFactoryTest {
    @Test
    public void createsUrgentDurableStateDataItemAtVersionedPath() {
        byte[] payload = "snapshot".getBytes(StandardCharsets.UTF_8);

        PutDataRequest request = new WearDataRequestFactory().create(payload);

        assertEquals("wear", request.getUri().getScheme());
        assertEquals(WearDataRequestFactory.STATE_PATH, request.getUri().getPath());
        assertTrue(request.isUrgent());
        assertArrayEquals(
                payload,
                DataMap.fromByteArray(request.getData())
                        .getByteArray(WearDataRequestFactory.PAYLOAD_KEY));
    }
}
