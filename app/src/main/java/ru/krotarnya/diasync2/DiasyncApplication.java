package ru.krotarnya.diasync2;

import android.app.Application;
import androidx.room.Room;
import com.google.gson.Gson;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import ru.krotarnya.diasync2.data.BootstrapRepository;
import ru.krotarnya.diasync2.data.DataPointMapper;
import ru.krotarnya.diasync2.data.api.HttpBootstrapDataSource;
import ru.krotarnya.diasync2.data.local.AppDatabase;
import ru.krotarnya.diasync2.presentation.StatusPresenter;
import ru.krotarnya.diasync2.settings.AppPreferences;

public final class DiasyncApplication extends Application {
    private BootstrapRepository bootstrapRepository;
    private AppPreferences preferences;
    private StatusPresenter statusPresenter;
    private ExecutorService ioExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        AppDatabase database = Room.databaseBuilder(this, AppDatabase.class, "diasync.db").build();
        Clock clock = Clock.systemUTC();
        bootstrapRepository = new BootstrapRepository(
                new HttpBootstrapDataSource(new OkHttpClient(), new Gson()),
                database.bootstrapDao(),
                new DataPointMapper(),
                clock);
        preferences = new AppPreferences(this);
        statusPresenter = new StatusPresenter(clock);
        ioExecutor = Executors.newSingleThreadExecutor();
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

    public ExecutorService ioExecutor() {
        return ioExecutor;
    }
}
