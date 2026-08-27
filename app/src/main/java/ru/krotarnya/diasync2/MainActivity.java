package ru.krotarnya.diasync2;

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
import androidx.appcompat.app.AppCompatActivity;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.data.BootstrapResult;
import ru.krotarnya.diasync2.presentation.StatusState;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.ConfigurationValidator;

public final class MainActivity extends AppCompatActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        application = (DiasyncApplication) getApplication();
        bindViews();
        configureUnitSpinner();
        refresh.setOnClickListener(ignored -> startBootstrap());

        Optional<AppConfiguration> saved = application.preferences().load();
        if (saved.isEmpty()) {
            render(application.statusPresenter().configurationMissing());
            return;
        }
        AppConfiguration configuration = saved.get();
        populate(configuration);
        loadLocal(configuration);
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

    private void startBootstrap() {
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
        int generation = operationGeneration.incrementAndGet();
        render(application.statusPresenter().loading());
        application.ioExecutor().execute(() -> {
            BootstrapResult result = application.bootstrapRepository().bootstrap(
                    configuration.baseUrl(),
                    configuration.userId());
            StatusState state = application.statusPresenter().bootstrap(
                    result,
                    configuration.unit(),
                    configuration.useCalibration());
            runOnUiThread(() -> renderIfCurrent(generation, state));
        });
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
        value.setVisibility(
                state.kind() == StatusState.Kind.LATEST_VALUE ? View.VISIBLE : View.GONE);
        timestamp.setVisibility(
                state.kind() == StatusState.Kind.LATEST_VALUE ? View.VISIBLE : View.GONE);
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
