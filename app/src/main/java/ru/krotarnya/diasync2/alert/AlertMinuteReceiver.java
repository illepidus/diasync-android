package ru.krotarnya.diasync2.alert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import ru.krotarnya.diasync2.DiasyncApplication;

public final class AlertMinuteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !AlertMinuteScheduler.ACTION_MINUTE_TICK.equals(intent.getAction())) {
            return;
        }
        DiasyncApplication application = (DiasyncApplication) context.getApplicationContext();
        AlertMinuteScheduler scheduler = new AlertMinuteScheduler();
        if (application.preferences().monitoringEnabled()) {
            application.phoneAlertController().checkAsync();
            scheduler.schedule(context);
        } else {
            scheduler.cancel(context);
        }
    }
}
