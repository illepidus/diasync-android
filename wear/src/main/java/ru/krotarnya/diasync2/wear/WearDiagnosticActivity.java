package ru.krotarnya.diasync2.wear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import java.time.Clock;
import ru.krotarnya.diasync2.R;

public final class WearDiagnosticActivity extends Activity {
    private static final long REFRESH_INTERVAL_MILLIS = 1_000L;
    public static final String ACTION_STATE_UPDATED =
            "ru.krotarnya.diasync2.action.WEAR_STATE_UPDATED";

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTicker = new Runnable() {
        @Override
        public void run() {
            renderLastKnownState();
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MILLIS);
        }
    };

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
        refreshHandler.removeCallbacks(refreshTicker);
        refreshTicker.run();
    }

    @Override
    protected void onPause() {
        refreshHandler.removeCallbacks(refreshTicker);
        super.onPause();
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
        ((TextView) findViewById(R.id.wear_snapshot_time)).setText(state.reading());
        ((TextView) findViewById(R.id.wear_snapshot_details)).setText(state.snapshot());
        ((TextView) findViewById(R.id.wear_display_details)).setText(state.display());
        ((TextView) findViewById(R.id.wear_alert_details)).setText(state.alerts());
        ((TextView) findViewById(R.id.wear_last_error)).setText(state.error());
        findViewById(R.id.wear_error_section).setVisibility(
                state.error().isBlank() ? View.GONE : View.VISIBLE);
    }
}
