package ru.krotarnya.diasync2.sync;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.net.NetworkCapabilities;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class NetworkValidationTest {
    @Test
    public void internetCapabilityAloneIsNotAValidatedConnection() {
        NetworkCapabilities capabilities = new NetworkCapabilities();
        shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        assertFalse(NetworkValidation.isValidated(capabilities));
        shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        assertTrue(NetworkValidation.isValidated(capabilities));
    }

    @Test
    public void missingNetworkIsNotValidated() {
        assertFalse(NetworkValidation.isValidated(null));
    }
}
