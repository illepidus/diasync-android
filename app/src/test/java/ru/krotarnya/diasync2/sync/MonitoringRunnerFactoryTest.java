package ru.krotarnya.diasync2.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Test;
import ru.krotarnya.diasync2.settings.AppMode;
import ru.krotarnya.diasync2.settings.AppConfiguration;
import ru.krotarnya.diasync2.settings.GraphWindow;
import ru.krotarnya.diasync2.common.GlucoseUnit;
import ru.krotarnya.diasync2.master.MasterOutboxDrainer;
import ru.krotarnya.diasync2.master.MasterUploadWork;

public class MonitoringRunnerFactoryTest {
    @Test
    public void masterWaitsForXdripWithoutCreatingSlaveNetworkWork() throws Exception {
        AtomicBoolean slaveWorkCreated = new AtomicBoolean();
        NoOpMasterWork masterWork = new NoOpMasterWork();
        RecordingListener listener = new RecordingListener();
        MonitoringRunner runner = MonitoringRunnerFactory.create(
                configuration(AppMode.MASTER),
                () -> {
                    slaveWorkCreated.set(true);
                    throw new AssertionError("Master must not create slave network work");
                },
                () -> masterWork,
                () -> true,
                listener);
        Thread thread = new Thread(runner);

        thread.start();

        assertTrue(listener.changed.await(1, TimeUnit.SECONDS));
        assertTrue(listener.states.contains(SyncConnectionState.WAITING_FOR_XDRIP));
        assertFalse(listener.states.contains(SyncConnectionState.UPLOADING));
        assertFalse(slaveWorkCreated.get());

        runner.stop();
        thread.join(1_000L);
        assertFalse(thread.isAlive());
        assertTrue(masterWork.cancelled.get());
    }

    @Test
    public void slaveCreatesOnlySlaveRunnerWork() {
        AtomicBoolean slaveWorkCreated = new AtomicBoolean();

        MonitoringRunner runner = MonitoringRunnerFactory.create(
                configuration(AppMode.SLAVE),
                () -> {
                    slaveWorkCreated.set(true);
                    return new NoOpSyncWork();
                },
                () -> {
                    throw new AssertionError("Slave must not create master upload work");
                },
                () -> true,
                new RecordingListener());

        assertTrue(runner instanceof SyncRunner);
        assertTrue(slaveWorkCreated.get());
    }

    private static final class RecordingListener implements MonitoringRunner.Listener {
        private final List<SyncConnectionState> states = new CopyOnWriteArrayList<>();
        private final CountDownLatch changed = new CountDownLatch(1);

        @Override
        public void onStateChanged(SyncConnectionState state) {
            states.add(state);
            changed.countDown();
        }

        @Override
        public void onDataCommitted() {
        }
    }

    private static final class NoOpSyncWork implements SyncWork {
        @Override
        public ru.krotarnya.diasync2.data.BootstrapResult bootstrap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ru.krotarnya.diasync2.data.BootstrapResult reconcile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ru.krotarnya.diasync2.data.LongPollResult poll(java.time.Instant since) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancelActiveCall() {
        }
    }

    private static final class NoOpMasterWork implements MasterUploadWork {
        private final AtomicBoolean cancelled = new AtomicBoolean();

        @Override
        public MasterOutboxDrainer.Result drainOnce(
                AppConfiguration configuration,
                Runnable onUploadStarted
        ) {
            return new MasterOutboxDrainer.Result(
                    MasterOutboxDrainer.Kind.IDLE,
                    0,
                    null);
        }

        @Override
        public void cancelActiveCall() {
            cancelled.set(true);
        }
    }

    private static AppConfiguration configuration(AppMode mode) {
        return new AppConfiguration(
                mode,
                "https://example.test/",
                "secret",
                GlucoseUnit.MMOL_L,
                true,
                70.0,
                180.0,
                GraphWindow.THIRTY_MINUTES,
                true,
                false,
                true);
    }
}
