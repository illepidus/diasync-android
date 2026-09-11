package ru.krotarnya.diasync2.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import ru.krotarnya.diasync2.settings.AppMode;

public class MonitoringRunnerFactoryTest {
    @Test
    public void masterWaitsForXdripWithoutCreatingSlaveNetworkWork() throws Exception {
        AtomicBoolean slaveWorkCreated = new AtomicBoolean();
        RecordingListener listener = new RecordingListener();
        MonitoringRunner runner = MonitoringRunnerFactory.create(
                AppMode.MASTER,
                () -> {
                    slaveWorkCreated.set(true);
                    throw new AssertionError("Master must not create slave network work");
                },
                listener);
        Thread thread = new Thread(runner);

        thread.start();

        assertTrue(listener.changed.await(1, TimeUnit.SECONDS));
        assertEquals(List.of(SyncConnectionState.WAITING_FOR_XDRIP), listener.states);
        assertFalse(slaveWorkCreated.get());

        runner.stop();
        thread.join(1_000L);
        assertFalse(thread.isAlive());
    }

    @Test
    public void slaveCreatesOnlySlaveRunnerWork() {
        AtomicBoolean slaveWorkCreated = new AtomicBoolean();

        MonitoringRunner runner = MonitoringRunnerFactory.create(
                AppMode.SLAVE,
                () -> {
                    slaveWorkCreated.set(true);
                    return new NoOpSyncWork();
                },
                new RecordingListener());

        assertTrue(runner instanceof SyncRunner);
        assertTrue(slaveWorkCreated.get());
    }

    private static final class RecordingListener implements MonitoringRunner.Listener {
        private final List<SyncConnectionState> states = new ArrayList<>();
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
}
