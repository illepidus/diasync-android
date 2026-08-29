package ru.krotarnya.diasync2.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

public final class WearAlertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !WearAlertScheduler.ACTION_CHECK.equals(intent.getAction())) {
            return;
        }
        LastKnownWearStateRepository repository = LastKnownWearStateRepository.create(context);
        WearSnapshot snapshot = repository.load().orElse(null);
        if (snapshot == null) {
            new WearAlertScheduler().cancel(context);
            return;
        }
        boolean complicationChanged = WearAlertController.create(context).onSnapshot(snapshot);
        if (complicationChanged) {
            DiasyncComplicationDataSourceService.requestUpdate(context);
        }
    }
}
