package ru.krotarnya.diasync2.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import ru.krotarnya.diasync2.master.MasterUploadScheduler;

public final class MonitoringRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            MonitoringRestartController.restart(context);
            MasterUploadScheduler.schedule(context);
        }
    }
}
