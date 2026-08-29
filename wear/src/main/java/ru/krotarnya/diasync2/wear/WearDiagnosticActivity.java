package ru.krotarnya.diasync2.wear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import java.time.Clock;
import ru.krotarnya.diasync2.R;

public final class WearDiagnosticActivity extends Activity {
    public static final String ACTION_STATE_UPDATED =
            "ru.krotarnya.diasync2.action.WEAR_STATE_UPDATED";

    private final BroadcastReceiver stateUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            renderLastKnownState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_diagnostic);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderLastKnownState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(
                stateUpdatedReceiver,
                new IntentFilter(ACTION_STATE_UPDATED),
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(stateUpdatedReceiver);
        super.onStop();
    }

    private void renderLastKnownState() {
        WearReceiveDiagnostics diagnostics = new WearReceiveDiagnostics(this);
        WearDiagnosticState state = new WearDiagnosticPresenter(Clock.systemUTC()).present(
                LastKnownWearStateRepository.create(this).load(), diagnostics.receivedAt(),
                diagnostics.lastError(),
                new SharedPreferencesWearAlertStateStore(this).read().dataPhase());
        ((TextView) findViewById(R.id.wear_latest_value)).setText(state.headline());
        ((TextView) findViewById(R.id.wear_snapshot_time)).setText(state.details());
        ((TextView) findViewById(R.id.wear_last_error)).setText(state.error());
    }
}
