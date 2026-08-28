package ru.krotarnya.diasync2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
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
import ru.krotarnya.diasync2.settings.AlertSettings;
import ru.krotarnya.diasync2.settings.ConfigurationValidator;
import ru.krotarnya.diasync2.settings.GraphWindow;
import ru.krotarnya.diasync2.settings.SnoozeOption;
import ru.krotarnya.diasync2.settings.SnoozeCountdown;
import ru.krotarnya.diasync2.settings.ThresholdDisplay;
import ru.krotarnya.diasync2.settings.WidgetSettings;
import ru.krotarnya.diasync2.sync.MonitoringService;
import ru.krotarnya.diasync2.sync.PhoneUpdateCoordinator;
import ru.krotarnya.diasync2.sync.SyncConnectionState;
import ru.krotarnya.diasync2.widget.DiasyncWidgetProvider;

public final class MainActivity extends AppCompatActivity implements PhoneUpdateCoordinator.Listener {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 100;

    private final AtomicInteger operationGeneration = new AtomicInteger();
    private final ConfigurationValidator configurationValidator = new ConfigurationValidator();
    private final ThresholdDisplay thresholdDisplay = new ThresholdDisplay();
    private final Handler snoozeHandler = new Handler(Looper.getMainLooper());
    private final Runnable snoozeTicker = this::updateSnoozeCountdown;

    private DiasyncApplication application;
    private EditText backendUrl;
    private EditText userId;
    private Spinner unit;
    private CheckBox useCalibration;
    private EditText lowThreshold;
    private EditText highThreshold;
    private Spinner widgetGraphWindow;
    private CheckBox widgetGraphZones;
    private CheckBox widgetGraphLines;
    private CheckBox widgetTrendArrow;
    private CheckBox lowAlertEnabled;
    private CheckBox highAlertEnabled;
    private CheckBox noDataAlertEnabled;
    private Spinner snoozeDuration;
    private Button snoozeToggle;
    private TextView snoozeStatus;
    private TextView status;
    private TextView value;
    private TextView timestamp;
    private ProgressBar progress;
    private Button monitoringToggle;
    private TextView monitoringStatus;
    private AppConfiguration pendingStartConfiguration;
    private GlucoseUnit displayedThresholdUnit;
    private double lowMgDl;
    private double highMgDl;
    private boolean bindingWidgetSettings;
    private boolean bindingAlertSettings;
    private boolean monitoringActive;
    private SnoozeCountdown snoozeCountdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        application = (DiasyncApplication) getApplication();
        snoozeCountdown = new SnoozeCountdown(application.clock());
        bindViews();
        configureUnitSpinner();
        configureGraphWindowSpinner();
        configureSnoozeSpinner();
        snoozeDuration.setSelection(application.preferences().loadSnoozeOption().ordinal());
        monitoringToggle.setOnClickListener(ignored -> toggleMonitoring());
        snoozeToggle.setOnClickListener(ignored -> toggleSnooze());

        populateWidgetSettings(application.preferences().loadWidgetSettings());
        populateAlertSettings(application.preferences().loadAlertSettings());
        Optional<AppConfiguration> saved = application.preferences().load();
        if (saved.isEmpty()) {
            render(application.statusPresenter().configurationMissing());
        } else {
            AppConfiguration configuration = saved.get();
            populateCredentials(configuration);
            loadLocal(configuration);
            if (application.preferences().monitoringEnabled()) {
                MonitoringService.start(this);
            }
        }
        configureWidgetSettingsListeners();
        configureAlertSettingsListeners();
        configureSnoozeSettingsListener();
        renderSnoozeCountdown();
        renderMonitoringState(application.preferences().syncConnectionState());
    }

    @Override
    protected void onStart() {
        super.onStart();
        application.phoneUpdateCoordinator().register(this);
        startSnoozeTicker();
        renderMonitoringState(application.preferences().syncConnectionState());
    }

    @Override
    protected void onStop() {
        snoozeHandler.removeCallbacks(snoozeTicker);
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
        lowThreshold = findViewById(R.id.low_threshold);
        highThreshold = findViewById(R.id.high_threshold);
        widgetGraphWindow = findViewById(R.id.widget_graph_window);
        widgetGraphZones = findViewById(R.id.widget_graph_zones);
        widgetGraphLines = findViewById(R.id.widget_graph_lines);
        widgetTrendArrow = findViewById(R.id.widget_trend_arrow);
        lowAlertEnabled = findViewById(R.id.low_alert_enabled);
        highAlertEnabled = findViewById(R.id.high_alert_enabled);
        noDataAlertEnabled = findViewById(R.id.no_data_alert_enabled);
        snoozeDuration = findViewById(R.id.snooze_duration);
        snoozeToggle = findViewById(R.id.snooze_toggle);
        snoozeStatus = findViewById(R.id.snooze_status);
        status = findViewById(R.id.status_text);
        value = findViewById(R.id.latest_value);
        timestamp = findViewById(R.id.latest_timestamp);
        progress = findViewById(R.id.progress);
        monitoringToggle = findViewById(R.id.monitoring_toggle);
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

    private void configureGraphWindowSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.widget_graph_windows,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        widgetGraphWindow.setAdapter(adapter);
    }

    private void configureSnoozeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.snooze_options,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        snoozeDuration.setAdapter(adapter);
    }

    private void populateCredentials(AppConfiguration configuration) {
        backendUrl.setText(configuration.baseUrl());
        userId.setText(configuration.userId());
    }

    private void populateWidgetSettings(WidgetSettings settings) {
        bindingWidgetSettings = true;
        displayedThresholdUnit = settings.unit();
        lowMgDl = settings.lowMgDl();
        highMgDl = settings.highMgDl();
        unit.setSelection(settings.unit() == GlucoseUnit.MMOL_L ? 0 : 1);
        useCalibration.setChecked(settings.useCalibration());
        widgetGraphWindow.setSelection(switch (settings.graphWindow()) {
            case THIRTY_MINUTES -> 0;
            case ONE_HOUR -> 1;
            case THREE_HOURS -> 2;
        });
        widgetGraphZones.setChecked(settings.graphZones());
        widgetGraphLines.setChecked(settings.graphLines());
        widgetTrendArrow.setChecked(settings.trendArrow());
        updateThresholdFields();
        bindingWidgetSettings = false;
    }

    private void populateAlertSettings(AlertSettings settings) {
        bindingAlertSettings = true;
        lowAlertEnabled.setChecked(settings.lowEnabled());
        highAlertEnabled.setChecked(settings.highEnabled());
        noDataAlertEnabled.setChecked(settings.noDataEnabled());
        bindingAlertSettings = false;
    }

    private void configureWidgetSettingsListeners() {
        unit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GlucoseUnit selected = selectedUnit();
                if (bindingWidgetSettings || selected == displayedThresholdUnit) {
                    return;
                }
                displayedThresholdUnit = selected;
                updateThresholdFields();
                persistWidgetSettings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        widgetGraphWindow.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!bindingWidgetSettings) {
                    persistWidgetSettings();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        useCalibration.setOnCheckedChangeListener((button, checked) -> persistWidgetSettings());
        widgetGraphZones.setOnCheckedChangeListener((button, checked) -> persistWidgetSettings());
        widgetGraphLines.setOnCheckedChangeListener((button, checked) -> persistWidgetSettings());
        widgetTrendArrow.setOnCheckedChangeListener((button, checked) -> persistWidgetSettings());
        TextWatcher thresholdWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                applyThresholdInputsIfValid();
            }
        };
        lowThreshold.addTextChangedListener(thresholdWatcher);
        highThreshold.addTextChangedListener(thresholdWatcher);
    }

    private void configureAlertSettingsListeners() {
        lowAlertEnabled.setOnCheckedChangeListener((button, checked) -> persistAlertSettings());
        highAlertEnabled.setOnCheckedChangeListener((button, checked) -> persistAlertSettings());
        noDataAlertEnabled.setOnCheckedChangeListener((button, checked) -> persistAlertSettings());
    }

    private void configureSnoozeSettingsListener() {
        snoozeDuration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                application.preferences().saveSnoozeOption(SnoozeOption.atPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void persistAlertSettings() {
        if (bindingAlertSettings) {
            return;
        }
        application.preferences().saveAlertSettings(new AlertSettings(
                lowAlertEnabled.isChecked(),
                highAlertEnabled.isChecked(),
                noDataAlertEnabled.isChecked()));
        application.publishWearState();
    }

    private void toggleSnooze() {
        if (alertsSnoozed()) {
            application.preferences().resumeAlerts();
            application.phoneAlertController().checkAsync();
        } else {
            SnoozeOption option = SnoozeOption.atPosition(snoozeDuration.getSelectedItemPosition());
            application.preferences().snoozeUntil(
                    application.clock().instant().plus(option.duration()));
        }
        application.publishWearState();
        startSnoozeTicker();
    }

    private void startSnoozeTicker() {
        snoozeHandler.removeCallbacks(snoozeTicker);
        updateSnoozeCountdown();
    }

    private void updateSnoozeCountdown() {
        boolean snoozed = renderSnoozeCountdown();
        if (snoozed) {
            snoozeHandler.postDelayed(snoozeTicker, 1_000L);
        }
    }

    private boolean renderSnoozeCountdown() {
        Optional<String> remaining = snoozeCountdown.remaining(
                application.preferences().snoozedUntil());
        snoozeStatus.setVisibility(remaining.isPresent() ? View.VISIBLE : View.GONE);
        remaining.ifPresent(value -> snoozeStatus.setText(
                getString(R.string.alerts_snoozed_for, value)));
        snoozeToggle.setText(remaining.isPresent()
                ? R.string.resume_alerts
                : R.string.snooze_alerts);
        return remaining.isPresent();
    }

    private boolean alertsSnoozed() {
        return snoozeCountdown.remaining(application.preferences().snoozedUntil()).isPresent();
    }

    private void updateThresholdFields() {
        bindingWidgetSettings = true;
        lowThreshold.setHint(displayedThresholdUnit == GlucoseUnit.MMOL_L
                ? R.string.low_threshold_mmol
                : R.string.low_threshold_mgdl);
        highThreshold.setHint(displayedThresholdUnit == GlucoseUnit.MMOL_L
                ? R.string.high_threshold_mmol
                : R.string.high_threshold_mgdl);
        lowThreshold.setText(thresholdDisplay.format(lowMgDl, displayedThresholdUnit));
        highThreshold.setText(thresholdDisplay.format(highMgDl, displayedThresholdUnit));
        bindingWidgetSettings = false;
    }

    private void applyThresholdInputsIfValid() {
        if (bindingWidgetSettings) {
            return;
        }
        try {
            double candidateLow = thresholdDisplay.parseMgDl(
                    lowThreshold.getText().toString(),
                    displayedThresholdUnit,
                    "Low threshold");
            double candidateHigh = thresholdDisplay.parseMgDl(
                    highThreshold.getText().toString(),
                    displayedThresholdUnit,
                    "High threshold");
            if (candidateLow >= candidateHigh) {
                return;
            }
            lowMgDl = candidateLow;
            highMgDl = candidateHigh;
            persistWidgetSettings();
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void requireValidThresholdInputs() {
        double candidateLow = thresholdDisplay.parseMgDl(
                lowThreshold.getText().toString(),
                displayedThresholdUnit,
                "Low threshold");
        double candidateHigh = thresholdDisplay.parseMgDl(
                highThreshold.getText().toString(),
                displayedThresholdUnit,
                "High threshold");
        if (candidateLow >= candidateHigh) {
            throw new IllegalArgumentException("Low threshold must be below high threshold");
        }
        lowMgDl = candidateLow;
        highMgDl = candidateHigh;
    }

    private void persistWidgetSettings() {
        if (bindingWidgetSettings) {
            return;
        }
        WidgetSettings settings = currentWidgetSettings();
        application.preferences().saveWidgetSettings(settings);
        DiasyncWidgetProvider.requestUpdate(this);
        application.publishWearState();
    }

    private WidgetSettings currentWidgetSettings() {
        return new WidgetSettings(
                selectedUnit(),
                useCalibration.isChecked(),
                lowMgDl,
                highMgDl,
                selectedGraphWindow(),
                widgetGraphZones.isChecked(),
                widgetGraphLines.isChecked(),
                widgetTrendArrow.isChecked());
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
            requireValidThresholdInputs();
            configuration = configurationValidator.validate(
                    backendUrl.getText().toString(),
                    userId.getText().toString(),
                    selectedUnit(),
                    useCalibration.isChecked(),
                    Double.toString(lowMgDl),
                    Double.toString(highMgDl),
                    selectedGraphWindow(),
                    widgetGraphZones.isChecked(),
                    widgetGraphLines.isChecked(),
                    widgetTrendArrow.isChecked());
        } catch (IllegalArgumentException exception) {
            status.setText(exception.getMessage());
            return;
        }
        application.preferences().save(configuration);
        DiasyncWidgetProvider.requestUpdate(this);
        application.publishWearState();
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
        renderMonitoringState(SyncConnectionState.DISABLED);
    }

    private void toggleMonitoring() {
        if (monitoringActive) {
            stopMonitoring();
        } else {
            startMonitoring();
        }
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
        monitoringActive = state != SyncConnectionState.DISABLED;
        monitoringStatus.setText(switch (state) {
            case DISABLED -> R.string.monitoring_disabled;
            case CONNECTING -> R.string.monitoring_connecting;
            case CONNECTED -> R.string.monitoring_connected;
        });
        monitoringToggle.setText(monitoringActive
                ? R.string.stop_monitoring
                : R.string.start_monitoring);
        monitoringToggle.setEnabled(true);
    }

    private GlucoseUnit selectedUnit() {
        return unit.getSelectedItemPosition() == 0
                ? GlucoseUnit.MMOL_L
                : GlucoseUnit.MG_DL;
    }

    private GraphWindow selectedGraphWindow() {
        return switch (widgetGraphWindow.getSelectedItemPosition()) {
            case 1 -> GraphWindow.ONE_HOUR;
            case 2 -> GraphWindow.THREE_HOURS;
            default -> GraphWindow.THIRTY_MINUTES;
        };
    }

    private void renderIfCurrent(int generation, StatusState state) {
        if (generation == operationGeneration.get() && !isDestroyed()) {
            render(state);
        }
    }

    private void render(StatusState state) {
        boolean loading = state.kind() == StatusState.Kind.LOADING;
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        monitoringToggle.setEnabled(monitoringActive || !loading);
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
