package ru.krotarnya.diasync2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.presentation.StatusState;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.ConfigurationValidator;
import ru.krotarnya.diasync2.sync.MonitoringService;
import ru.krotarnya.diasync2.sync.PhoneUpdateCoordinator;
import ru.krotarnya.diasync2.sync.SyncConnectionState;

public final class MainActivity extends AppCompatActivity implements PhoneUpdateCoordinator.Listener {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 100;

    private final AtomicInteger operationGeneration = new AtomicInteger();
    private final ConfigurationValidator configurationValidator = new ConfigurationValidator();

    private DiasyncApplication application;
    private EditText backendUrl;
    private EditText userId;
    private Spinner unit;
    private CheckBox useCalibration;
    private TextView status;
    private TextView value;
    private TextView timestamp;
    private ProgressBar progress;
    private Button refresh;
    private Button stopMonitoring;
    private TextView monitoringStatus;
    private AppConfiguration pendingStartConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        application = (DiasyncApplication) getApplication();
        bindViews();
        configureUnitSpinner();
        refresh.setOnClickListener(ignored -> startMonitoring());
        stopMonitoring.setOnClickListener(ignored -> stopMonitoring());

        Optional<AppConfiguration> saved = application.preferences().load();
        if (saved.isEmpty()) {
            render(application.statusPresenter().configurationMissing());
            return;
        }
        AppConfiguration configuration = saved.get();
        populate(configuration);
        loadLocal(configuration);
        if (application.preferences().monitoringEnabled()) {
            MonitoringService.start(this);
        }
        renderMonitoringState(application.preferences().syncConnectionState());
    }

    @Override
    protected void onStart() {
        super.onStart();
        application.phoneUpdateCoordinator().register(this);
        renderMonitoringState(application.preferences().syncConnectionState());
    }

    @Override
    protected void onStop() {
        application.phoneUpdateCoordinator().unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        operationGeneration.incrementAndGet();
        super.onDestroy();
    }

    private void bindViews() {
        backendUrl = findViewById(R.id.backend_url);
        userId = findViewById(R.id.user_id);
        userId.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        unit = findViewById(R.id.glucose_unit);
        useCalibration = findViewById(R.id.use_calibration);
        status = findViewById(R.id.status_text);
        value = findViewById(R.id.latest_value);
        timestamp = findViewById(R.id.latest_timestamp);
        progress = findViewById(R.id.progress);
        refresh = findViewById(R.id.refresh);
        stopMonitoring = findViewById(R.id.stop_monitoring);
        monitoringStatus = findViewById(R.id.monitoring_status);
    }

    private void configureUnitSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.glucose_units,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unit.setAdapter(adapter);
    }

    private void populate(AppConfiguration configuration) {
        backendUrl.setText(configuration.baseUrl());
        userId.setText(configuration.userId());
        unit.setSelection(configuration.unit() == GlucoseUnit.MMOL_L ? 0 : 1);
        useCalibration.setChecked(configuration.useCalibration());
    }

    private void loadLocal(AppConfiguration configuration) {
        int generation = operationGeneration.incrementAndGet();
        render(application.statusPresenter().loading());
        application.ioExecutor().execute(() -> {
            DataPoint local = application.bootstrapRepository()
                    .latestLocalSensorPoint(configuration.userId());
            StatusState state = application.statusPresenter().local(
                    local,
                    configuration.unit(),
                    configuration.useCalibration());
            runOnUiThread(() -> renderIfCurrent(generation, state));
        });
    }

    private void startMonitoring() {
        AppConfiguration configuration;
        try {
            configuration = configurationValidator.validate(
                    backendUrl.getText().toString(),
                    userId.getText().toString(),
                    selectedUnit(),
                    useCalibration.isChecked());
        } catch (IllegalArgumentException exception) {
            status.setText(exception.getMessage());
            return;
        }
        application.preferences().save(configuration);
        pendingStartConfiguration = configuration;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
            return;
        }
        beginMonitoring(configuration);
    }

    private void beginMonitoring(AppConfiguration configuration) {
        pendingStartConfiguration = null;
        operationGeneration.incrementAndGet();
        render(application.statusPresenter().loading());
        application.preferences().setMonitoringEnabled(true);
        application.phoneUpdateCoordinator().stateChanged(SyncConnectionState.CONNECTING);
        MonitoringService.start(this);
    }

    private void stopMonitoring() {
        pendingStartConfiguration = null;
        MonitoringService.stop(this);
        renderMonitoringState(SyncConnectionState.STOPPED);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != NOTIFICATION_PERMISSION_REQUEST || pendingStartConfiguration == null) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            beginMonitoring(pendingStartConfiguration);
        } else {
            pendingStartConfiguration = null;
            monitoringStatus.setText(R.string.monitoring_permission_required);
        }
    }

    @Override
    public void onSyncStateChanged(SyncConnectionState state, boolean dataChanged) {
        renderMonitoringState(state);
        if (dataChanged) {
            application.preferences().load().ifPresent(this::loadLocal);
        }
    }

    private void renderMonitoringState(SyncConnectionState state) {
        monitoringStatus.setText(switch (state) {
            case STOPPED -> R.string.monitoring_stopped;
            case CONNECTING -> R.string.monitoring_connecting;
            case WAITING -> R.string.monitoring_waiting;
            case RETRYING -> R.string.monitoring_retrying;
        });
        stopMonitoring.setEnabled(state != SyncConnectionState.STOPPED);
    }

    private GlucoseUnit selectedUnit() {
        return unit.getSelectedItemPosition() == 0
                ? GlucoseUnit.MMOL_L
                : GlucoseUnit.MG_DL;
    }

    private void renderIfCurrent(int generation, StatusState state) {
        if (generation == operationGeneration.get() && !isDestroyed()) {
            render(state);
        }
    }

    private void render(StatusState state) {
        boolean loading = state.kind() == StatusState.Kind.LOADING;
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        refresh.setEnabled(!loading);
        int visibility = state.kind() == StatusState.Kind.LATEST_VALUE ? View.VISIBLE : View.GONE;
        value.setVisibility(visibility);
        timestamp.setVisibility(visibility);
        if (state.kind() == StatusState.Kind.LATEST_VALUE) {
            value.setText(getString(R.string.value_with_unit, state.value(), state.unit()));
            timestamp.setText(getString(
                    R.string.timestamp_with_age,
                    state.timestamp(),
                    state.age()));
        }
        status.setText(statusText(state.kind()));
    }

    private int statusText(StatusState.Kind kind) {
        return switch (kind) {
            case CONFIGURATION_MISSING -> R.string.status_configuration_missing;
            case LOADING -> R.string.status_loading;
            case LATEST_VALUE -> R.string.status_latest;
            case NO_DATA -> R.string.status_no_data;
            case CONNECTION_ERROR -> R.string.status_connection_error;
            case HTTP_ERROR -> R.string.status_http_error;
            case PARSE_ERROR -> R.string.status_parse_error;
            case INVALID_DATA -> R.string.status_invalid_data;
            case STORAGE_ERROR -> R.string.status_storage_error;
        };
    }
}
