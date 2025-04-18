package ru.krotarnya.diasync.common.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.function.Consumer;

import ru.krotarnya.diasync.common.intent.NewDataIntent;
import ru.krotarnya.diasync.common.model.DataPoint;
import ru.krotarnya.diasync.common.repository.DiasyncDatabase;

public class NewDataReceiver extends BroadcastReceiver {
    private final DiasyncDatabase db;
    private final Consumer<DataPoint> callback;

    public NewDataReceiver(DiasyncDatabase db, Consumer<DataPoint> callback) {
        this.db = db;
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        NewDataIntent.parse(intent)
                .map(NewDataIntent::userId)
                .flatMap(userId -> db.dataPointDao().findLast(userId))
                .ifPresent(callback);
    }

    public IntentFilter intentFilter() {
        return NewDataIntent.filter();
    }
}
