package ru.krotarnya.diasync2.sync;

import android.net.NetworkCapabilities;

final class NetworkValidation {
    private NetworkValidation() {
    }

    static boolean isValidated(NetworkCapabilities capabilities) {
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
