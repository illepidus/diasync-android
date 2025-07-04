package ru.krotarnya.diasync.common.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import ru.krotarnya.diasync.common.events.BatteryStatusChanged;
import ru.krotarnya.diasync.common.model.BatteryStatus;

public class BatteryStatusReceiver extends BroadcastReceiver {
    public static final String TAG = BatteryStatusReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "BATTERY STATUS UPDATED");
        if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
            new BatteryStatusChanged(new BatteryStatus(intent)).post();
        }
    }

    public void register(Context context) {
        context.registerReceiver(this, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }
    
    public void unregister(Context context) {
        context.unregisterReceiver(this);
    }
}
