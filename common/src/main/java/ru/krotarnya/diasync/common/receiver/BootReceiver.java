package ru.krotarnya.diasync.common.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.krotarnya.diasync.common.service.DataSyncService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, DataSyncService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}