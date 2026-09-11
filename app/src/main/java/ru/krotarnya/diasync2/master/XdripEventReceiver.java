package ru.krotarnya.diasync2.master;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.concurrent.RejectedExecutionException;
import ru.krotarnya.diasync2.DiasyncApplication;

public final class XdripEventReceiver extends BroadcastReceiver {
    public static final String ACTION_XDRIP_EVENT =
            "ru.krotarnya.diasync2.action.XDRIP_EVENT";
    public static final String EXTRA_PAYLOAD = "payload";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_XDRIP_EVENT.equals(intent.getAction())) {
            return;
        }
        PendingResult pendingResult = goAsync();
        DiasyncApplication application = (DiasyncApplication) context.getApplicationContext();
        try {
            application.masterIngestExecutor().execute(() -> {
                try {
                    application.masterEventIngestor().ingest(
                            intent.getStringExtra(EXTRA_PAYLOAD));
                } catch (RuntimeException exception) {
                    application.recordMasterIngestDiagnostic("RECEIVER_FAILED");
                } finally {
                    pendingResult.finish();
                }
            });
        } catch (RejectedExecutionException exception) {
            application.recordMasterIngestDiagnostic("EXECUTOR_SATURATED");
            pendingResult.finish();
        }
    }
}
