package ru.krotarnya.diasync2.master;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class XdripEventReceiverManifestTest {
    private static final String PERMISSION =
            "ru.krotarnya.diasync2.permission.SEND_XDRIP_EVENT";

    @Test
    public void exportedReceiverRequiresSignaturePermission() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager packageManager = context.getPackageManager();

        ActivityInfo receiver = packageManager.getReceiverInfo(
                new ComponentName(context, XdripEventReceiver.class),
                0);
        PermissionInfo permission = packageManager.getPermissionInfo(
                PERMISSION,
                0);

        assertTrue(receiver.exported);
        assertEquals(PERMISSION, receiver.permission);
        assertEquals(
                PermissionInfo.PROTECTION_SIGNATURE,
                permission.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE);
    }
}
