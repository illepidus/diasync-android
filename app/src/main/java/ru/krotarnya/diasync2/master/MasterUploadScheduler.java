package ru.krotarnya.diasync2.master;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public final class MasterUploadScheduler {
    static final int JOB_ID = 0xD1A5;

    private MasterUploadScheduler() {
    }

    public static void schedule(Context context) {
        Context applicationContext = context.getApplicationContext();
        JobInfo job = new JobInfo.Builder(
                JOB_ID,
                new ComponentName(applicationContext, MasterUploadJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setBackoffCriteria(10_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build();
        applicationContext.getSystemService(JobScheduler.class).schedule(job);
    }
}
