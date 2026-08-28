package ru.krotarnya.diasync2.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.Test;
import ru.krotarnya.diasync2.data.BootstrapResult;
import ru.krotarnya.diasync2.data.LongPollResult;

public class SyncRunnerTest {
    private static final Instant INITIAL_SINCE = Instant.parse("2026-08-27T12:00:00Z");
    private static final Instant SERVER_CURSOR = Instant.parse("2026-08-27T12:01:00Z");

    @Test
    public void emptySuccessImmediatelyPollsAgainAndStaysConnected() {
        FakeWork work = new FakeWork(
                BootstrapResult.noData(INITIAL_SINCE),
                LongPollResult.of(LongPollResult.Kind.EMPTY),
                LongPollResult.of(LongPollResult.Kind.CANCELLED));
        RecordingListener listener = new RecordingListener();
        List<Duration> sleeps = new ArrayList<>();
        SyncRunner runner = runner(work, listener, sleeps);

        runner.run();

        assertEquals(List.of(INITIAL_SINCE, INITIAL_SINCE), work.pollCursors);
        assertEquals(
                List.of(SyncConnectionState.CONNECTING, SyncConnectionState.CONNECTED),
                listener.states);
        assertEquals(0, sleeps.size());
    }

    @Test
    public void dataSwitchesToMaximumServerCursor() {
        FakeWork work = new FakeWork(
                BootstrapResult.noData(INITIAL_SINCE),
                LongPollResult.data(SERVER_CURSOR),
                LongPollResult.of(LongPollResult.Kind.CANCELLED));
        RecordingListener listener = new RecordingListener();

        runner(work, listener, new ArrayList<>()).run();

        assertEquals(List.of(INITIAL_SINCE, SERVER_CURSOR), work.pollCursors);
        assertEquals(2, listener.commits);
    }

    @Test
    public void errorBacksOffThenSuccessfulRoundTripResetsBackoff() {
        FakeWork work = new FakeWork(
                BootstrapResult.noData(INITIAL_SINCE),
                LongPollResult.of(LongPollResult.Kind.CONNECTION_ERROR),
                LongPollResult.of(LongPollResult.Kind.EMPTY),
                LongPollResult.of(LongPollResult.Kind.HTTP_ERROR),
                LongPollResult.of(LongPollResult.Kind.CANCELLED));
        List<Duration> sleeps = new ArrayList<>();
        RecordingListener listener = new RecordingListener();

        runner(work, listener, sleeps).run();

        assertEquals(List.of(Duration.ofMillis(500), Duration.ofMillis(500)), sleeps);
        assertEquals(
                List.of(
                        SyncConnectionState.CONNECTING,
                        SyncConnectionState.CONNECTED,
                        SyncConnectionState.CONNECTING,
                        SyncConnectionState.CONNECTED,
                        SyncConnectionState.CONNECTING),
                listener.states);
    }

    @Test
    public void stopCancelsActiveWorkAndPreventsFurtherPolling() {
        FakeWork work = new FakeWork(BootstrapResult.noData(INITIAL_SINCE));
        SyncRunner[] holder = new SyncRunner[1];
        SyncRunner.Listener listener = new SyncRunner.Listener() {
            @Override
            public void onStateChanged(SyncConnectionState state) {
                if (state == SyncConnectionState.CONNECTED) {
                    holder[0].stop();
                }
            }

            @Override
            public void onDataCommitted() { }
        };
        holder[0] = runner(work, listener, new ArrayList<>());

        holder[0].run();

        assertTrue(work.cancelled);
        assertEquals(0, work.pollCursors.size());
    }

    private SyncRunner runner(
            FakeWork work,
            SyncRunner.Listener listener,
            List<Duration> sleeps
    ) {
        return new SyncRunner(
                work,
                new BackoffPolicy(() -> 0.0),
                sleeps::add,
                listener);
    }

    private static final class RecordingListener implements SyncRunner.Listener {
        private final List<SyncConnectionState> states = new ArrayList<>();
        private int commits;

        @Override
        public void onStateChanged(SyncConnectionState state) {
            states.add(state);
        }

        @Override
        public void onDataCommitted() {
            commits++;
        }
    }

    private static final class FakeWork implements SyncWork {
        private final BootstrapResult bootstrapResult;
        private final Deque<LongPollResult> pollResults = new ArrayDeque<>();
        private final List<Instant> pollCursors = new ArrayList<>();
        private boolean cancelled;

        private FakeWork(BootstrapResult bootstrapResult, LongPollResult... pollResults) {
            this.bootstrapResult = bootstrapResult;
            this.pollResults.addAll(List.of(pollResults));
        }

        @Override
        public BootstrapResult bootstrap() {
            return bootstrapResult;
        }

        @Override
        public LongPollResult poll(Instant since) {
            pollCursors.add(since);
            return pollResults.removeFirst();
        }

        @Override
        public void cancelActiveCall() {
            cancelled = true;
        }
    }
}
