package ru.krotarnya.diasync2;

import android.app.Application;
import androidx.room.Room;
import com.google.gson.Gson;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import ru.krotarnya.diasync2.common.TrendCalculator;
import ru.krotarnya.diasync2.data.BootstrapRepository;
import ru.krotarnya.diasync2.data.DataPointMapper;
import ru.krotarnya.diasync2.data.api.HttpBootstrapDataSource;
import ru.krotarnya.diasync2.data.LongPollRepository;
import ru.krotarnya.diasync2.data.api.HttpLongPollDataSource;
import ru.krotarnya.diasync2.data.local.AppDatabase;
import ru.krotarnya.diasync2.presentation.StatusPresenter;
import ru.krotarnya.diasync2.settings.AppPreferences;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.sync.PhoneUpdateCoordinator;
import ru.krotarnya.diasync2.sync.RepositorySyncWork;
import ru.krotarnya.diasync2.sync.SyncWork;
import ru.krotarnya.diasync2.widget.WidgetPresenter;

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

    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(this, AppDatabase.class, "diasync.db").build();
        clock = Clock.systemUTC();
        bootstrapRepository = new BootstrapRepository(
                new HttpBootstrapDataSource(new OkHttpClient(), new Gson()),
                database.bootstrapDao(),
                new DataPointMapper(),
                clock);
        preferences = new AppPreferences(this);
        phoneUpdateCoordinator = new PhoneUpdateCoordinator(this, preferences);
        statusPresenter = new StatusPresenter(clock);
        widgetPresenter = new WidgetPresenter(clock, new TrendCalculator());
        ioExecutor = Executors.newSingleThreadExecutor();
        widgetExecutor = Executors.newSingleThreadExecutor();
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

    public PhoneUpdateCoordinator phoneUpdateCoordinator() {
        return phoneUpdateCoordinator;
    }

    public SyncWork createSyncWork(AppConfiguration configuration) {
        OkHttpClient syncClient = new OkHttpClient.Builder()
                .readTimeout(Duration.ofSeconds(90))
                .build();
        DataPointMapper mapper = new DataPointMapper();
        return new RepositorySyncWork(
                configuration,
                new BootstrapRepository(
                        new HttpBootstrapDataSource(syncClient, new Gson()),
                        database.bootstrapDao(),
                        mapper,
                        clock),
                new LongPollRepository(
                        new HttpLongPollDataSource(syncClient, new Gson()),
                        database.bootstrapDao(),
                        mapper,
                        clock),
                syncClient);
    }
}
