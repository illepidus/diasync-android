package ru.krotarnya.diasync2.sync;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import ru.krotarnya.diasync2.data.BootstrapResult;
import ru.krotarnya.diasync2.data.LongPollResult;

public final class SyncRunner implements Runnable {
    public interface Listener {
        void onStateChanged(SyncConnectionState state);

        void onDataCommitted();
    }

    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final SyncWork work;
    private final BackoffPolicy backoff;
    private final Sleeper sleeper;
    private final Listener listener;
    private final AtomicBoolean stopped = new AtomicBoolean();
    private SyncConnectionState lastState;

    public SyncRunner(
            SyncWork work,
            BackoffPolicy backoff,
            Sleeper sleeper,
            Listener listener
    ) {
        this.work = Objects.requireNonNull(work);
        this.backoff = Objects.requireNonNull(backoff);
        this.sleeper = Objects.requireNonNull(sleeper);
        this.listener = Objects.requireNonNull(listener);
    }

    @Override
    public void run() {
        changeState(SyncConnectionState.CONNECTING);
        Instant since = bootstrapUntilSuccessful();
        while (!stopped.get() && since != null) {
            LongPollResult result = work.poll(since);
            if (stopped.get() || result.kind() == LongPollResult.Kind.CANCELLED) {
                return;
            }
            if (result.kind() == LongPollResult.Kind.DATA) {
                since = result.cursor();
                backoff.reset();
                changeState(SyncConnectionState.CONNECTED);
                listener.onDataCommitted();
            } else if (result.kind() == LongPollResult.Kind.EMPTY) {
                backoff.reset();
                changeState(SyncConnectionState.CONNECTED);
            } else if (result.kind() == LongPollResult.Kind.INVALID_DATA) {
                if (!retryAfterFailure()) {
                    return;
                }
                since = reconcileUntilSuccessful();
            } else if (!retryAfterFailure()) {
                return;
            }
        }
    }

    public void stop() {
        stopped.set(true);
        work.cancelActiveCall();
    }

    private Instant bootstrapUntilSuccessful() {
        return bootstrapUntilSuccessful(false);
    }

    private Instant reconcileUntilSuccessful() {
        return bootstrapUntilSuccessful(true);
    }

    private Instant bootstrapUntilSuccessful(boolean reconciliation) {
        while (!stopped.get()) {
            BootstrapResult result = reconciliation ? work.reconcile() : work.bootstrap();
            if (stopped.get()) {
                return null;
            }
            if (result.kind() == BootstrapResult.Kind.SUCCESS
                    || result.kind() == BootstrapResult.Kind.NO_DATA) {
                backoff.reset();
                changeState(SyncConnectionState.CONNECTED);
                listener.onDataCommitted();
                return result.initialPollSince();
            }
            if (!retryAfterFailure()) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean retryAfterFailure() {
        changeState(SyncConnectionState.RETRYING);
        try {
            sleeper.sleep(backoff.nextDelay());
            if (stopped.get()) {
                return false;
            }
            changeState(SyncConnectionState.CONNECTING);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void changeState(SyncConnectionState state) {
        if (state != lastState) {
            lastState = state;
            listener.onStateChanged(state);
        }
    }
}
