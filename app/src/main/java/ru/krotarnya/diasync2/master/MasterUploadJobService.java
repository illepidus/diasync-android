package ru.krotarnya.diasync2.master;

import android.app.job.JobParameters;
import android.app.job.JobService;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import ru.krotarnya.diasync2.DiasyncApplication;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.AppMode;

public final class MasterUploadJobService extends JobService {
    private ExecutorService executor;
    private Future<?> work;
    private final AtomicBoolean stopped = new AtomicBoolean();

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        DiasyncApplication application = (DiasyncApplication) getApplication();
        Optional<AppConfiguration> configuration = application.preferences().load();
        if (configuration.isEmpty() || configuration.get().mode() != AppMode.MASTER) {
            return false;
        }
        stopped.set(false);
        work = executor.submit(() -> runJob(application, configuration.get(), params));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        DiasyncApplication application = (DiasyncApplication) getApplication();
        stopped.set(true);
        application.masterOutboxDrainer().cancelActiveCall();
        if (work != null) {
            work.cancel(true);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void runJob(
            DiasyncApplication application,
            AppConfiguration configuration,
            JobParameters params
    ) {
        MasterOutboxDrainer.Result result;
        do {
            result = application.masterOutboxDrainer().drainOnce(configuration);
            application.recordMasterUploadResult(result);
        } while (!Thread.currentThread().isInterrupted()
                && result.kind() == MasterOutboxDrainer.Kind.DELIVERED);
        if (!stopped.get()) {
            jobFinished(params, result.kind() == MasterOutboxDrainer.Kind.RETRY_SCHEDULED);
        }
    }
}
