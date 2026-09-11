package ru.krotarnya.diasync2.master;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MasterUploadSchedulerTest {
    @Test
    public void schedulesPersistedNetworkJob() {
        Context context = ApplicationProvider.getApplicationContext();

        MasterUploadScheduler.schedule(context);

        JobInfo job = context.getSystemService(JobScheduler.class)
                .getPendingJob(MasterUploadScheduler.JOB_ID);
        assertEquals(JobInfo.NETWORK_TYPE_ANY, job.getNetworkType());
        assertEquals(true, job.isPersisted());
        assertEquals(
                new ComponentName(context, MasterUploadJobService.class),
                job.getService());
    }

    @Test
    public void jobServiceIsPrivateAndRequiresPlatformBindingPermission() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ServiceInfo service = context.getPackageManager().getServiceInfo(
                new ComponentName(context, MasterUploadJobService.class),
                PackageManager.ComponentInfoFlags.of(0));

        assertFalse(service.exported);
        assertEquals("android.permission.BIND_JOB_SERVICE", service.permission);
    }
}
