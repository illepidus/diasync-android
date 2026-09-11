package ru.krotarnya.diasync2;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import androidx.room.Room;
import com.google.gson.Gson;
import com.google.android.gms.wearable.Wearable;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import ru.krotarnya.diasync2.alert.AlertNotificationPublisher;
import ru.krotarnya.diasync2.alert.AlertSoundPlayer;
import ru.krotarnya.diasync2.alert.PhoneAlertController;
import ru.krotarnya.diasync2.common.AlertEvaluator;
import ru.krotarnya.diasync2.common.TrendCalculator;
import ru.krotarnya.diasync2.data.BootstrapRepository;
import ru.krotarnya.diasync2.data.DataPointMapper;
import ru.krotarnya.diasync2.data.api.HttpBootstrapDataSource;
import ru.krotarnya.diasync2.data.LongPollRepository;
import ru.krotarnya.diasync2.data.api.HttpLongPollDataSource;
import ru.krotarnya.diasync2.data.api.HttpMasterUploadDataSource;
import ru.krotarnya.diasync2.data.local.AppDatabase;
import ru.krotarnya.diasync2.presentation.StatusPresenter;
import ru.krotarnya.diasync2.presentation.DataReceiptDiagnostics;
import ru.krotarnya.diasync2.presentation.DiagnosticEventLog;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.sync.PhoneUpdateCoordinator;
import ru.krotarnya.diasync2.sync.RepositorySyncWork;
import ru.krotarnya.diasync2.sync.SyncWork;
import ru.krotarnya.diasync2.sync.SyncTimeouts;
import ru.krotarnya.diasync2.widget.DiasyncWidgetProvider;
import ru.krotarnya.diasync2.widget.WidgetPresenter;
import ru.krotarnya.diasync2.wear.WearSnapshotBuilder;
import ru.krotarnya.diasync2.wear.WearDataRequestFactory;
import ru.krotarnya.diasync2.wear.WearStatePublisher;
import ru.krotarnya.diasync2.wear.WearSyncDiagnostics;
import ru.krotarnya.diasync2.common.wear.WearSnapshotCodec;
import ru.krotarnya.diasync2.common.master.XdripEventCodec;
import ru.krotarnya.diasync2.master.MasterEventIngestor;
import ru.krotarnya.diasync2.master.MasterEventRepository;
import ru.krotarnya.diasync2.master.MasterOutboxDrainer;
import ru.krotarnya.diasync2.master.MasterRetryDelay;
import ru.krotarnya.diasync2.master.MasterUploadScheduler;
import ru.krotarnya.diasync2.master.MasterUploadWork;
import ru.krotarnya.diasync2.data.local.MasterEventDao;

public final class DiasyncApplication extends Application {
    private BootstrapRepository bootstrapRepository;
    private AppPreferences preferences;
    private StatusPresenter statusPresenter;
    private WidgetPresenter widgetPresenter;
    private PhoneUpdateCoordinator phoneUpdateCoordinator;
    private AppDatabase database;
    private Clock clock;
    private ExecutorService ioExecutor;
    private ExecutorService widgetExecutor;
    private ExecutorService alertExecutor;
    private ExecutorService wearExecutor;
    private PhoneAlertController phoneAlertController;
    private WearStatePublisher wearStatePublisher;
    private DataReceiptDiagnostics dataReceiptDiagnostics;
    private DiagnosticEventLog diagnosticEventLog;
    private WearSyncDiagnostics wearSyncDiagnostics;
    private ThreadPoolExecutor masterIngestExecutor;
    private MasterEventIngestor masterEventIngestor;
    private MasterOutboxDrainer masterOutboxDrainer;

    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(this, AppDatabase.class, AppDatabase.NAME)
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
                .build();
        clock = Clock.systemUTC();
        dataReceiptDiagnostics = new DataReceiptDiagnostics(this);
        diagnosticEventLog = new DiagnosticEventLog(this);
        wearSyncDiagnostics = new WearSyncDiagnostics(this, diagnosticEventLog);
        bootstrapRepository = new BootstrapRepository(
                new HttpBootstrapDataSource(new OkHttpClient(), new Gson()),
                database.bootstrapDao(),
                new DataPointMapper(),
                clock,
                dataReceiptDiagnostics::record);
        preferences = new AppPreferences(this);
        statusPresenter = new StatusPresenter(clock);
        widgetPresenter = new WidgetPresenter(clock, new TrendCalculator());
        ioExecutor = Executors.newSingleThreadExecutor();
        widgetExecutor = Executors.newSingleThreadExecutor();
        alertExecutor = Executors.newSingleThreadExecutor();
        wearExecutor = Executors.newSingleThreadExecutor();
        wearStatePublisher = new WearStatePublisher(
                preferences,
                new WearSnapshotBuilder(
                        bootstrapRepository::latestLocalSensorPointsSince,
                        new TrendCalculator(),
                        clock),
                new WearSnapshotCodec(),
                Wearable.getDataClient(this),
                new WearDataRequestFactory(),
                wearSyncDiagnostics,
                wearExecutor,
                clock);
        AlertSoundPlayer alertSoundPlayer = new AlertSoundPlayer(this);
        AlertNotificationPublisher alertNotificationPublisher =
                new AlertNotificationPublisher(this);
        phoneAlertController = new PhoneAlertController(
                preferences,
                bootstrapRepository::latestLocalSensorPoints,
                new AlertEvaluator(clock, preferences.lastAlertAt()),
                alertSoundPlayer::play,
                alertNotificationPublisher::show,
                alertNotificationPublisher::hide,
                wearStatePublisher::publishAlert,
                alertExecutor,
                clock);
        phoneUpdateCoordinator = new PhoneUpdateCoordinator(
                this,
                preferences,
                phoneAlertController::checkAsync,
                wearStatePublisher::publishState,
                diagnosticEventLog,
                clock);
        masterIngestExecutor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(32),
                new ThreadPoolExecutor.AbortPolicy());
        OkHttpClient masterUploadClient = new OkHttpClient();
        masterOutboxDrainer = new MasterOutboxDrainer(
                database.masterEventDao(),
                new HttpMasterUploadDataSource(masterUploadClient, new Gson()),
                new XdripEventCodec(),
                clock,
                new MasterRetryDelay(Math::random));
        masterEventIngestor = new MasterEventIngestor(
                preferences::monitoringEnabled,
                preferences::load,
                new XdripEventCodec(),
                new MasterEventRepository(database.masterEventDao(), clock),
                () -> {
                    dataReceiptDiagnostics.record(clock.instant());
                    phoneUpdateCoordinator.dataCommitted();
                },
                () -> MasterUploadScheduler.schedule(this),
                this::recordMasterIngestDiagnostic);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DiasyncWidgetProvider.requestUpdate(this);
    }

    public BootstrapRepository bootstrapRepository() {
        return bootstrapRepository;
    }

    public AppPreferences preferences() {
        return preferences;
    }

    public StatusPresenter statusPresenter() {
        return statusPresenter;
    }

    public WidgetPresenter widgetPresenter() {
        return widgetPresenter;
    }

    public ExecutorService ioExecutor() {
        return ioExecutor;
    }

    public ExecutorService widgetExecutor() {
        return widgetExecutor;
    }

    public Clock clock() {
        return clock;
    }

    public PhoneUpdateCoordinator phoneUpdateCoordinator() {
        return phoneUpdateCoordinator;
    }

    public PhoneAlertController phoneAlertController() {
        return phoneAlertController;
    }

    public void publishWearState() {
        wearStatePublisher.publishState();
    }

    public DataReceiptDiagnostics dataReceiptDiagnostics() {
        return dataReceiptDiagnostics;
    }

    public DiagnosticEventLog diagnosticEventLog() {
        return diagnosticEventLog;
    }

    public WearSyncDiagnostics wearSyncDiagnostics() {
        return wearSyncDiagnostics;
    }

    public Executor masterIngestExecutor() {
        return masterIngestExecutor;
    }

    public MasterEventIngestor masterEventIngestor() {
        return masterEventIngestor;
    }

    public MasterEventDao masterEventDao() {
        return database.masterEventDao();
    }

    public MasterOutboxDrainer masterOutboxDrainer() {
        return masterOutboxDrainer;
    }

    public MasterUploadWork masterUploadWork() {
        return new MasterUploadWork() {
            @Override
            public MasterOutboxDrainer.Result drainOnce(AppConfiguration configuration) {
                MasterOutboxDrainer.Result result = masterOutboxDrainer.drainOnce(configuration);
                recordMasterUploadResult(result);
                return result;
            }

            @Override
            public void cancelActiveCall() {
                masterOutboxDrainer.cancelActiveCall();
            }
        };
    }

    public void recordMasterUploadResult(MasterOutboxDrainer.Result result) {
        if (result.diagnosticCode() != null) {
            diagnosticEventLog.record("Master upload", result.diagnosticCode(), clock.instant());
        }
    }

    public void recordMasterIngestDiagnostic(String code) {
        diagnosticEventLog.record("Master ingest", code, clock.instant());
    }

    public SyncWork createSyncWork(AppConfiguration configuration) {
        boolean debug = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        SyncTimeouts timeouts = SyncTimeouts.forBuild(debug);
        OkHttpClient syncClient = new OkHttpClient.Builder()
                .readTimeout(timeouts.clientRead())
                .build();
        DataPointMapper mapper = new DataPointMapper();
        return new RepositorySyncWork(
                configuration,
                new BootstrapRepository(
                        new HttpBootstrapDataSource(syncClient, new Gson()),
                        database.bootstrapDao(),
                        mapper,
                        clock,
                        dataReceiptDiagnostics::record),
                new LongPollRepository(
                        new HttpLongPollDataSource(
                                syncClient,
                                new Gson(),
                                timeouts.serverLongPoll()),
                        database.bootstrapDao(),
                        mapper,
                        clock,
                        dataReceiptDiagnostics::record),
                syncClient);
    }
}
