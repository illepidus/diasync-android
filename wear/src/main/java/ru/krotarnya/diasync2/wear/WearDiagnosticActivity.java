package ru.krotarnya.diasync2.wear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import java.util.Optional;
import ru.krotarnya.diasync2.R;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.common.wear.WearGlucosePoint;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

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
        render(LastKnownWearStateRepository.create(this).load());
    }

    private void render(Optional<WearSnapshot> state) {
        TextView latestValue = findViewById(R.id.wear_latest_value);
        TextView snapshotTime = findViewById(R.id.wear_snapshot_time);
        if (state.isEmpty() || state.get().points().isEmpty()) {
            latestValue.setText(R.string.wear_no_data);
            snapshotTime.setText("");
            return;
        }
        WearSnapshot snapshot = state.get();
        WearGlucosePoint latest = snapshot.points().get(0);
        GlucoseUnit unit = snapshot.display().unit();
        latestValue.setText(getString(
                R.string.wear_last_value,
                unit.formatFromMgDl(latest.displayMgDl(snapshot.display().useCalibration())),
                unit.symbol()));
        snapshotTime.setText(getString(
                R.string.wear_generated_at,
                snapshot.generatedAt().toString()));
    }
}
