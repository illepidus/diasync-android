package ru.krotarnya.diasync2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.slider.Slider;
import com.google.android.gms.wearable.Wearable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.data.SyncDiagnosticsData;
import ru.krotarnya.diasync2.presentation.StatusState;
import ru.krotarnya.diasync2.navigation.PhoneScreen;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AlertSettings;
import ru.krotarnya.diasync2.settings.ConfigurationValidator;
import ru.krotarnya.diasync2.settings.GraphWindow;
import ru.krotarnya.diasync2.settings.SnoozeOption;
import ru.krotarnya.diasync2.settings.SnoozeCountdown;
import ru.krotarnya.diasync2.settings.ThresholdDisplay;
import ru.krotarnya.diasync2.settings.WatchSettings;
import ru.krotarnya.diasync2.settings.WidgetSettings;
import ru.krotarnya.diasync2.settings.WidgetClickAction;
import ru.krotarnya.diasync2.sync.MonitoringService;
import ru.krotarnya.diasync2.sync.MonitoringRestartController;
import ru.krotarnya.diasync2.sync.PhoneUpdateCoordinator;
import ru.krotarnya.diasync2.sync.SyncConnectionState;
import ru.krotarnya.diasync2.widget.DiasyncWidgetProvider;
import ru.krotarnya.diasync2.wear.WearSyncDiagnostics;

public final class MainActivity extends AppCompatActivity implements PhoneUpdateCoordinator.Listener {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 100;
    public static final String EXTRA_SCREEN = "ru.krotarnya.diasync2.extra.SCREEN";
    private static final String STATE_SCREEN = "screen";

    private final AtomicInteger operationGeneration = new AtomicInteger();
    private final ConfigurationValidator configurationValidator = new ConfigurationValidator();
    private final ThresholdDisplay thresholdDisplay = new ThresholdDisplay();
    private final Handler snoozeHandler = new Handler(Looper.getMainLooper());
    private final Runnable snoozeTicker = this::updateSnoozeCountdown;
    private final Handler diagnosticsHandler = new Handler(Looper.getMainLooper());
    private final Runnable diagnosticsTicker = this::updateDiagnostics;

    private DiasyncApplication application;
    private EditText backendUrl;
    private EditText userId;
    private Spinner unit;
    private CheckBox useCalibration;
    private EditText lowThreshold;
    private EditText highThreshold;
    private TextInputLayout lowThresholdContainer;
    private TextInputLayout highThresholdContainer;
    private Slider widgetGraphWindow;
    private TextView widgetGraphWindowLabel;
    private AutoCompleteTextView widgetSingleClickAction;
    private AutoCompleteTextView widgetDoubleClickAction;
    private CheckBox widgetGraphZones;
    private CheckBox widgetGraphLines;
    private CheckBox widgetTrendArrow;
    private Slider watchGraphWindow;
    private TextView watchGraphWindowLabel;
    private CheckBox watchGraphZones;
    private CheckBox watchGraphLines;
    private CheckBox watchTrendArrow;
    private CheckBox lowAlertEnabled;
    private CheckBox highAlertEnabled;
    private CheckBox noDataAlertEnabled;
    private CheckBox snoozeWearAlerts;
    private Slider snoozeDuration;
    private TextView snoozeDurationLabel;
    private Button snoozeToggle;
    private TextView snoozeStatus;
    private TextView status;
    private TextView value;
    private TextView timestamp;
    private ProgressBar progress;
    private Button monitoringToggle;
    private TextView monitoringStatus;
    private TextView diagnosticsData;
    private TextView diagnosticsSync;
    private TextView diagnosticsWear;
    private TextView diagnosticsEvents;
    private AppConfiguration pendingStartConfiguration;
    private GlucoseUnit displayedThresholdUnit;
    private double lowMgDl;
    private double highMgDl;
    private boolean bindingWidgetSettings;
    private boolean bindingWatchSettings;
    private boolean bindingAlertSettings;
    private boolean monitoringActive;
    private boolean activityStarted;
    private boolean diagnosticsLoaded;
    private boolean wearDiagnosticsLoaded;
    private int wearQueryGeneration;
    private SnoozeCountdown snoozeCountdown;
    private PhoneScreen currentScreen = PhoneScreen.STATUS;
    private StatusState lastRenderedStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        applySystemBarInsets();
        application = (DiasyncApplication) getApplication();
        snoozeCountdown = new SnoozeCountdown(application.clock());
        bindViews();
        configureNavigation();
        configureUnitSpinner();
        configureGraphWindowControls();
        configureWidgetClickActionMenus();
        configureSnoozeSlider();
        snoozeDuration.setValue(application.preferences().loadSnoozeOption().ordinal());
        updateSnoozeDurationLabel();
        monitoringToggle.setOnClickListener(ignored -> toggleMonitoring());
        snoozeToggle.setOnClickListener(ignored -> toggleSnooze());

        populateWidgetSettings(application.preferences().loadWidgetSettings());
        populateWidgetClickActions();
        populateWatchSettings(application.preferences().loadWatchSettings());
        populateAlertSettings(application.preferences().loadAlertSettings());
        Optional<AppConfiguration> saved = application.preferences().load();
        if (saved.isEmpty()) {
            render(application.statusPresenter().configurationMissing());
        } else {
            AppConfiguration configuration = saved.get();
            populateCredentials(configuration);
            loadLocal(configuration);
            if (application.preferences().monitoringEnabled()) {
                MonitoringRestartController.restart(this);
            }
        }
        configureWidgetSettingsListeners();
        configureWatchSettingsListeners();
        configureAlertSettingsListeners();
        configureSnoozeSettingsListener();
        renderSnoozeCountdown();
        renderMonitoringState(application.preferences().syncConnectionState());
        PhoneScreen initialScreen = savedInstanceState == null
                ? PhoneScreen.fromRoute(getIntent().getStringExtra(EXTRA_SCREEN))
                : PhoneScreen.fromRoute(savedInstanceState.getString(STATE_SCREEN));
        showScreen(initialScreen);
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.main);
        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    initialLeft + bars.left,
                    initialTop + bars.top,
                    initialRight + bars.right,
                    initialBottom + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_SCREEN, currentScreen.name());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showScreen(PhoneScreen.fromRoute(intent.getStringExtra(EXTRA_SCREEN)));
    }

    private void configureNavigation() {
        findViewById(R.id.open_connection).setOnClickListener(v -> showScreen(PhoneScreen.CONNECTION));
        findViewById(R.id.open_glucose).setOnClickListener(v -> showScreen(PhoneScreen.GLUCOSE));
        findViewById(R.id.open_widget).setOnClickListener(v -> showScreen(PhoneScreen.WIDGET));
        findViewById(R.id.open_watch).setOnClickListener(v -> showScreen(PhoneScreen.WATCH));
        findViewById(R.id.open_alerts).setOnClickListener(v -> showScreen(PhoneScreen.ALERTS));
        findViewById(R.id.open_diagnostics).setOnClickListener(v -> showScreen(PhoneScreen.DIAGNOSTICS));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentScreen != PhoneScreen.STATUS) {
                    showScreen(PhoneScreen.STATUS);
                } else {
                    moveTaskToBack(true);
                }
            }
        });
    }

    private void showScreen(PhoneScreen screen) {
        currentScreen = screen;
        findViewById(R.id.settings_menu).setVisibility(screen == PhoneScreen.STATUS ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.screen_title)).setText(switch (screen) {
            case STATUS -> R.string.status_title;
            case CONNECTION -> R.string.connection_settings_title;
            case GLUCOSE -> R.string.glucose_settings_title;
            case WIDGET -> R.string.widget_settings_title;
            case WATCH -> R.string.watch_settings_title;
            case ALERTS -> R.string.alert_settings_title;
            case DIAGNOSTICS -> R.string.diagnostics_title;
        });
        setVisible(screen == PhoneScreen.CONNECTION, R.id.backend_url_container, R.id.user_id_container,
                R.id.monitoring_status_panel, R.id.monitoring_toggle);
        setVisible(screen == PhoneScreen.GLUCOSE, R.id.glucose_unit,
                R.id.low_threshold_container, R.id.high_threshold_container,
                R.id.use_calibration);
        setVisible(screen == PhoneScreen.WIDGET, R.id.widget_window_block,
                R.id.widget_single_click_container, R.id.widget_double_click_container,
                R.id.widget_graph_zones, R.id.widget_graph_lines, R.id.widget_trend_arrow);
        setVisible(screen == PhoneScreen.WATCH, R.id.watch_window_block,
                R.id.watch_graph_zones, R.id.watch_graph_lines, R.id.watch_trend_arrow);
        setVisible(screen == PhoneScreen.ALERTS, R.id.snooze_block,
                R.id.low_alert_enabled, R.id.high_alert_enabled, R.id.no_data_alert_enabled);
        boolean diagnostics = screen == PhoneScreen.DIAGNOSTICS;
        setVisible(diagnostics, R.id.progress, R.id.diagnostics_content);
        if (lastRenderedStatus != null) {
            render(lastRenderedStatus);
        }
        startDiagnosticsTicker();
    }

    private void setVisible(boolean visible, int... ids) {
        for (int id : ids) {
            View view = findViewById(id);
            if (!visible && view != null) {
                view.setVisibility(View.GONE);
            } else if (visible && view != null && id != R.id.latest_value && id != R.id.latest_timestamp
                    && id != R.id.progress && id != R.id.snooze_status) {
                view.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityStarted = true;
        application.phoneUpdateCoordinator().register(this);
        startSnoozeTicker();
        renderMonitoringState(application.preferences().syncConnectionState());
        startDiagnosticsTicker();
    }

    @Override
    protected void onStop() {
        activityStarted = false;
        wearQueryGeneration++;
        snoozeHandler.removeCallbacks(snoozeTicker);
        diagnosticsHandler.removeCallbacks(diagnosticsTicker);
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
        lowThresholdContainer = findViewById(R.id.low_threshold_container);
        highThresholdContainer = findViewById(R.id.high_threshold_container);
        widgetGraphWindow = findViewById(R.id.widget_graph_window);
        widgetGraphWindowLabel = findViewById(R.id.widget_graph_window_label);
        widgetSingleClickAction = findViewById(R.id.widget_single_click_action);
        widgetDoubleClickAction = findViewById(R.id.widget_double_click_action);
        widgetGraphZones = findViewById(R.id.widget_graph_zones);
        widgetGraphLines = findViewById(R.id.widget_graph_lines);
        widgetTrendArrow = findViewById(R.id.widget_trend_arrow);
        watchGraphWindow = findViewById(R.id.watch_graph_window);
        watchGraphWindowLabel = findViewById(R.id.watch_graph_window_label);
        watchGraphZones = findViewById(R.id.watch_graph_zones);
        watchGraphLines = findViewById(R.id.watch_graph_lines);
        watchTrendArrow = findViewById(R.id.watch_trend_arrow);
        lowAlertEnabled = findViewById(R.id.low_alert_enabled);
        highAlertEnabled = findViewById(R.id.high_alert_enabled);
        noDataAlertEnabled = findViewById(R.id.no_data_alert_enabled);
        snoozeWearAlerts = findViewById(R.id.snooze_wear_alerts);
        snoozeDuration = findViewById(R.id.snooze_duration);
        snoozeDurationLabel = findViewById(R.id.snooze_duration_label);
        snoozeToggle = findViewById(R.id.snooze_toggle);
        snoozeStatus = findViewById(R.id.snooze_status);
        status = findViewById(R.id.status_text);
        value = findViewById(R.id.latest_value);
        timestamp = findViewById(R.id.latest_timestamp);
        progress = findViewById(R.id.progress);
        monitoringToggle = findViewById(R.id.monitoring_toggle);
        monitoringStatus = findViewById(R.id.monitoring_status);
        diagnosticsData = findViewById(R.id.diagnostics_data);
        diagnosticsSync = findViewById(R.id.diagnostics_sync);
        diagnosticsWear = findViewById(R.id.diagnostics_wear);
        diagnosticsEvents = findViewById(R.id.diagnostics_events);
    }

    private void configureUnitSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.glucose_units,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unit.setAdapter(adapter);
    }

    private void configureGraphWindowControls() {
        widgetGraphWindow.setValueFrom(0.0f);
        widgetGraphWindow.setValueTo(2.0f);
        widgetGraphWindow.setStepSize(1.0f);
        widgetGraphWindow.setLabelFormatter(value -> graphWindowLabel(Math.round(value)));
        watchGraphWindow.setValueFrom(0.0f);
        watchGraphWindow.setValueTo(2.0f);
        watchGraphWindow.setStepSize(1.0f);
        watchGraphWindow.setLabelFormatter(value -> graphWindowLabel(Math.round(value)));
    }

    private void configureWidgetClickActionMenus() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.widget_click_actions, android.R.layout.simple_dropdown_item_1line);
        widgetSingleClickAction.setAdapter(adapter);
        widgetDoubleClickAction.setAdapter(adapter);
        widgetSingleClickAction.setOnItemClickListener((parent, view, position, id) ->
                persistWidgetClickActions());
        widgetDoubleClickAction.setOnItemClickListener((parent, view, position, id) ->
                persistWidgetClickActions());
    }

    private void configureSnoozeSlider() {
        snoozeDuration.setValueFrom(0.0f);
        snoozeDuration.setValueTo(SnoozeOption.values().length - 1.0f);
        snoozeDuration.setStepSize(1.0f);
        snoozeDuration.setLabelFormatter(value -> snoozeLabel(Math.round(value)));
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
        widgetGraphWindow.setValue(switch (settings.graphWindow()) {
            case THIRTY_MINUTES -> 0;
            case ONE_HOUR -> 1;
            case THREE_HOURS -> 2;
        });
        updateWidgetGraphWindowLabel();
        widgetGraphZones.setChecked(settings.graphZones());
        widgetGraphLines.setChecked(settings.graphLines());
        widgetTrendArrow.setChecked(settings.trendArrow());
        updateThresholdFields();
        bindingWidgetSettings = false;
    }

    private void populateWidgetClickActions() {
        WidgetClickAction single = application.preferences().loadWidgetSingleClickAction();
        WidgetClickAction doubleClick = application.preferences().loadWidgetDoubleClickAction();
        String[] labels = getResources().getStringArray(R.array.widget_click_actions);
        widgetSingleClickAction.setText(labels[single.ordinal()], false);
        widgetDoubleClickAction.setText(labels[doubleClick.ordinal()], false);
    }

    private void populateWatchSettings(WatchSettings settings) {
        bindingWatchSettings = true;
        watchGraphWindow.setValue(graphWindowPosition(settings.graphWindow()));
        updateWatchGraphWindowLabel();
        watchGraphZones.setChecked(settings.graphZones());
        watchGraphLines.setChecked(settings.graphLines());
        watchTrendArrow.setChecked(settings.trendArrow());
        bindingWatchSettings = false;
    }

    private void populateAlertSettings(AlertSettings settings) {
        bindingAlertSettings = true;
        lowAlertEnabled.setChecked(settings.lowEnabled());
        highAlertEnabled.setChecked(settings.highEnabled());
        noDataAlertEnabled.setChecked(settings.noDataEnabled());
        snoozeWearAlerts.setChecked(application.preferences().snoozeWearAlerts());
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
                persistWidgetSettings(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        widgetGraphWindow.addOnChangeListener((slider, value, fromUser) -> {
            updateWidgetGraphWindowLabel();
            if (!bindingWidgetSettings) {
                persistWidgetSettings(false);
            }
        });
        useCalibration.setOnCheckedChangeListener(
                (button, checked) -> persistWidgetSettings(true));
        widgetGraphZones.setOnCheckedChangeListener(
                (button, checked) -> persistWidgetSettings(false));
        widgetGraphLines.setOnCheckedChangeListener(
                (button, checked) -> persistWidgetSettings(false));
        widgetTrendArrow.setOnCheckedChangeListener(
                (button, checked) -> persistWidgetSettings(false));
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
        lowThreshold.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                normalizeThresholdFieldsIfValid();
            }
        });
        highThreshold.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                normalizeThresholdFieldsIfValid();
            }
        });
    }

    private void configureWatchSettingsListeners() {
        watchGraphWindow.addOnChangeListener((slider, value, fromUser) -> {
            updateWatchGraphWindowLabel();
            persistWatchSettings();
        });
        watchGraphZones.setOnCheckedChangeListener((button, checked) -> persistWatchSettings());
        watchGraphLines.setOnCheckedChangeListener((button, checked) -> persistWatchSettings());
        watchTrendArrow.setOnCheckedChangeListener((button, checked) -> persistWatchSettings());
    }

    private void configureAlertSettingsListeners() {
        lowAlertEnabled.setOnCheckedChangeListener((button, checked) -> persistAlertSettings());
        highAlertEnabled.setOnCheckedChangeListener((button, checked) -> persistAlertSettings());
        noDataAlertEnabled.setOnCheckedChangeListener((button, checked) -> persistAlertSettings());
        snoozeWearAlerts.setOnCheckedChangeListener((button, checked) -> {
            if (bindingAlertSettings) {
                return;
            }
            application.preferences().saveSnoozeWearAlerts(checked);
            application.publishWearState();
        });
    }

    private void configureSnoozeSettingsListener() {
        snoozeDuration.addOnChangeListener((slider, value, fromUser) -> {
            application.preferences().saveSnoozeOption(
                    SnoozeOption.atPosition(Math.round(value)));
            updateSnoozeDurationLabel();
        });
    }

    private void updateSnoozeDurationLabel() {
        snoozeDurationLabel.setText(snoozeLabel(Math.round(snoozeDuration.getValue())));
    }

    private String snoozeLabel(int position) {
        String[] labels = getResources().getStringArray(R.array.snooze_options);
        return labels[Math.max(0, Math.min(position, labels.length - 1))];
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
            SnoozeOption option = SnoozeOption.atPosition(Math.round(snoozeDuration.getValue()));
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
        snoozeStatus.setText(remaining
                .map(value -> getString(R.string.alerts_snoozed_for, value))
                .orElseGet(() -> getString(R.string.alerts_active)));
        snoozeToggle.setText(remaining.isPresent()
                ? R.string.resume_alerts
                : R.string.snooze_alerts);
        int durationVisibility = remaining.isPresent() ? View.GONE : View.VISIBLE;
        snoozeDuration.setVisibility(durationVisibility);
        snoozeDurationLabel.setVisibility(durationVisibility);
        return remaining.isPresent();
    }

    private boolean alertsSnoozed() {
        return snoozeCountdown.remaining(application.preferences().snoozedUntil()).isPresent();
    }

    private void updateThresholdFields() {
        bindingWidgetSettings = true;
        lowThresholdContainer.setHint(getString(displayedThresholdUnit == GlucoseUnit.MMOL_L
                ? R.string.low_threshold_mmol
                : R.string.low_threshold_mgdl));
        highThresholdContainer.setHint(getString(displayedThresholdUnit == GlucoseUnit.MMOL_L
                ? R.string.high_threshold_mmol
                : R.string.high_threshold_mgdl));
        int inputType = InputType.TYPE_CLASS_NUMBER
                | (displayedThresholdUnit == GlucoseUnit.MMOL_L
                ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0);
        lowThreshold.setInputType(inputType);
        highThreshold.setInputType(inputType);
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
            thresholdDisplay.validateRange(candidateLow, candidateHigh, displayedThresholdUnit);
            lowThresholdContainer.setError(null);
            highThresholdContainer.setError(null);
            lowMgDl = candidateLow;
            highMgDl = candidateHigh;
            persistWidgetSettings(true);
        } catch (IllegalArgumentException exception) {
            showThresholdError(exception.getMessage());
        }
    }

    private void normalizeThresholdFieldsIfValid() {
        if (bindingWidgetSettings) {
            return;
        }
        try {
            requireValidThresholdInputs();
            updateThresholdFields();
            persistWidgetSettings(true);
        } catch (IllegalArgumentException exception) {
            showThresholdError(exception.getMessage());
        }
    }

    private void showThresholdError(String message) {
        lowThresholdContainer.setError(null);
        highThresholdContainer.setError(null);
        if (message != null && message.startsWith("Low threshold")) {
            lowThresholdContainer.setError(message);
        } else {
            highThresholdContainer.setError(message);
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
        thresholdDisplay.validateRange(candidateLow, candidateHigh, displayedThresholdUnit);
        lowMgDl = candidateLow;
        highMgDl = candidateHigh;
    }

    private void persistWidgetSettings(boolean publishWearState) {
        if (bindingWidgetSettings) {
            return;
        }
        WidgetSettings settings = currentWidgetSettings();
        application.preferences().saveWidgetSettings(settings);
        DiasyncWidgetProvider.requestUpdate(this);
        if (publishWearState) {
            application.publishWearState();
        }
    }

    private void persistWidgetClickActions() {
        application.preferences().saveWidgetClickActions(
                WidgetClickAction.atPosition(widgetClickPosition(widgetSingleClickAction)),
                WidgetClickAction.atPosition(widgetClickPosition(widgetDoubleClickAction)));
    }

    private int widgetClickPosition(AutoCompleteTextView view) {
        String[] labels = getResources().getStringArray(R.array.widget_click_actions);
        for (int position = 0; position < labels.length; position++) {
            if (labels[position].contentEquals(view.getText())) {
                return position;
            }
        }
        return 0;
    }

    private void updateWidgetGraphWindowLabel() {
        widgetGraphWindowLabel.setText(graphWindowLabel(Math.round(widgetGraphWindow.getValue())));
    }

    private void updateWatchGraphWindowLabel() {
        watchGraphWindowLabel.setText(graphWindowLabel(Math.round(watchGraphWindow.getValue())));
    }

    private String graphWindowLabel(int position) {
        String[] labels = getResources().getStringArray(R.array.widget_graph_windows);
        return labels[Math.max(0, Math.min(position, labels.length - 1))];
    }

    private void persistWatchSettings() {
        if (bindingWatchSettings) {
            return;
        }
        application.preferences().saveWatchSettings(new WatchSettings(
                selectedWatchGraphWindow(),
                watchGraphZones.isChecked(),
                watchGraphLines.isChecked(),
                watchTrendArrow.isChecked()));
        application.publishWearState();
    }

    private WidgetSettings currentWidgetSettings() {
        return new WidgetSettings(
                selectedUnit(),
                useCalibration.isChecked(),
                lowMgDl,
                highMgDl,
                selectedWidgetGraphWindow(),
                widgetGraphZones.isChecked(),
                widgetGraphLines.isChecked(),
                widgetTrendArrow.isChecked());
    }

    private void startDiagnosticsTicker() {
        diagnosticsHandler.removeCallbacks(diagnosticsTicker);
        if (activityStarted && currentScreen == PhoneScreen.DIAGNOSTICS) {
            updateDiagnostics();
        } else {
            wearQueryGeneration++;
        }
    }

    private void updateDiagnostics() {
        if (!activityStarted || currentScreen != PhoneScreen.DIAGNOSTICS) {
            return;
        }
        loadDiagnostics();
        diagnosticsHandler.postDelayed(diagnosticsTicker, 1_000L);
    }

    private void loadDiagnostics() {
        if (!diagnosticsLoaded) {
            diagnosticsData.setText("Loading…");
            diagnosticsSync.setText("Loading…");
            diagnosticsLoaded = true;
        }
        if (!wearDiagnosticsLoaded) {
            diagnosticsWear.setText(wearSendText() + "\nConnected watches: checking…");
        }
        List<String> events = application.diagnosticEventLog().latest();
        int from = Math.max(0, events.size() - 12);
        if (events.isEmpty()) {
            diagnosticsEvents.setText("No diagnostic events yet");
        } else {
            SpannableStringBuilder newestFirst = new SpannableStringBuilder();
            for (int index = events.size() - 1; index >= from; index--) {
                if (newestFirst.length() > 0) {
                    newestFirst.append('\n');
                }
                appendDiagnosticEvent(newestFirst, events.get(index));
            }
            diagnosticsEvents.setText(newestFirst);
        }
        Optional<AppConfiguration> configuration = application.preferences().load();
        if (configuration.isEmpty()) {
            diagnosticsData.setText("No configured data source");
            diagnosticsSync.setText("State: " + application.preferences().syncConnectionState());
        } else {
            application.ioExecutor().execute(() -> {
                SyncDiagnosticsData data = application.bootstrapRepository()
                        .diagnostics(configuration.get().userId());
                String dataText = diagnosticsDataText(data);
                String syncText = diagnosticsSyncText(data);
                runOnUiThread(() -> {
                    diagnosticsData.setText(dataText);
                    diagnosticsSync.setText(syncText);
                });
            });
        }
        int queryGeneration = ++wearQueryGeneration;
        Runnable timeout = () -> renderWearQueryUnavailable(queryGeneration);
        diagnosticsHandler.postDelayed(timeout, 1_000L);
        Wearable.getNodeClient(this).getConnectedNodes()
                .addOnSuccessListener(nodes -> {
                    if (queryGeneration != wearQueryGeneration) {
                        return;
                    }
                    diagnosticsHandler.removeCallbacks(timeout);
                    wearDiagnosticsLoaded = true;
                    renderWearDiagnostics(nodes);
                })
                .addOnFailureListener(ignored -> {
                    if (queryGeneration != wearQueryGeneration) {
                        return;
                    }
                    diagnosticsHandler.removeCallbacks(timeout);
                    renderWearQueryUnavailable(queryGeneration);
                });
    }

    private void renderWearQueryUnavailable(int queryGeneration) {
        if (queryGeneration != wearQueryGeneration
                || !activityStarted
                || currentScreen != PhoneScreen.DIAGNOSTICS) {
            return;
        }
        wearQueryGeneration++;
        wearDiagnosticsLoaded = true;
        diagnosticsWear.setText(wearSendText() + "\nConnected watches: unavailable");
    }

    private String diagnosticsDataText(SyncDiagnosticsData data) {
        Instant measurement = data.latestPoint() == null ? null : data.latestPoint().timestamp();
        Instant received = application.dataReceiptDiagnostics().receivedAt();
        return "Received by phone: " + formatInstant(received)
                + "\nMeasurement time: " + formatInstant(measurement)
                + "\nData age: " + formatAge(measurement);
    }

    private String diagnosticsSyncText(SyncDiagnosticsData data) {
        return "State: " + application.preferences().syncConnectionState()
                + "\nLast successful response: " + formatInstant(data.lastSuccessAt())
                + "\nCursor update time: " + formatInstant(data.cursorUpdateTimestamp())
                + "\nLast error: " + (data.lastError() == null ? "None" : data.lastError());
    }

    private void renderWearDiagnostics(List<com.google.android.gms.wearable.Node> nodes) {
        StringBuilder watches = new StringBuilder();
        for (com.google.android.gms.wearable.Node node : nodes) {
            if (watches.length() > 0) {
                watches.append(", ");
            }
            watches.append(node.getDisplayName());
        }
        diagnosticsWear.setText(wearSendText() + "\nConnected watches: "
                + (watches.length() == 0 ? "None connected" : watches));
    }

    private String wearSendText() {
        WearSyncDiagnostics diagnostics = application.wearSyncDiagnostics();
        Instant updatedAt = diagnostics.updatedAt();
        return "Last Data Layer send: " + diagnostics.state()
                + "\nUpdated: " + formatInstant(updatedAt.equals(Instant.EPOCH) ? null : updatedAt);
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "Never";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    private void appendDiagnosticEvent(SpannableStringBuilder target, String event) {
        int timestampEnd = event.indexOf(" · ");
        if (timestampEnd < 0) {
            target.append(event);
            return;
        }
        int moduleStart = timestampEnd + 3;
        int moduleEnd = event.indexOf(" · ", moduleStart);
        if (moduleEnd < 0) {
            target.append(event);
            return;
        }
        try {
            Instant timestamp = Instant.parse(event.substring(0, timestampEnd));
            target.append(formatInstant(timestamp)).append(' ');
            int spanStart = target.length();
            target.append(event, moduleStart, moduleEnd);
            target.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.brand_orange)),
                    spanStart,
                    target.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            target.append(' ').append(event.substring(moduleEnd + 3));
        } catch (RuntimeException ignored) {
            target.append(event);
        }
    }

    private String formatAge(Instant instant) {
        if (instant == null) {
            return "Unknown";
        }
        Duration age = Duration.between(instant, application.clock().instant());
        if (age.isNegative()) {
            return "From the future";
        }
        long totalSeconds = age.getSeconds();
        long hours = totalSeconds / 3_600;
        long minutes = totalSeconds % 3_600 / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
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
                    selectedWidgetGraphWindow(),
                    widgetGraphZones.isChecked(),
                    widgetGraphLines.isChecked(),
                    widgetTrendArrow.isChecked());
        } catch (IllegalArgumentException exception) {
            monitoringStatus.setText(exception.getMessage());
            backendUrl.setError(backendUrl.getText().toString().isBlank()
                    ? getString(R.string.required_field) : null);
            userId.setError(userId.getText().toString().isBlank()
                    ? getString(R.string.required_field) : null);
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
            case RETRYING -> R.string.monitoring_retrying;
            case BLOCKED -> R.string.monitoring_blocked;
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

    private GraphWindow selectedWatchGraphWindow() {
        return switch (Math.round(watchGraphWindow.getValue())) {
            case 1 -> GraphWindow.ONE_HOUR;
            case 2 -> GraphWindow.THREE_HOURS;
            default -> GraphWindow.THIRTY_MINUTES;
        };
    }

    private GraphWindow selectedWidgetGraphWindow() {
        return switch (Math.round(widgetGraphWindow.getValue())) {
            case 1 -> GraphWindow.ONE_HOUR;
            case 2 -> GraphWindow.THREE_HOURS;
            default -> GraphWindow.THIRTY_MINUTES;
        };
    }

    private int graphWindowPosition(GraphWindow graphWindow) {
        return switch (graphWindow) {
            case THIRTY_MINUTES -> 0;
            case ONE_HOUR -> 1;
            case THREE_HOURS -> 2;
        };
    }

    private void renderIfCurrent(int generation, StatusState state) {
        if (generation == operationGeneration.get() && !isDestroyed()) {
            render(state);
        }
    }

    void render(StatusState state) {
        lastRenderedStatus = state;
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
        progress.setVisibility(View.GONE);
        value.setVisibility(View.GONE);
        timestamp.setVisibility(View.GONE);
        status.setVisibility(View.GONE);
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
