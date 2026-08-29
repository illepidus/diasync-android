package ru.krotarnya.diasync2.wear;

import android.content.Context;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import ru.krotarnya.diasync2.common.AlertType;
import ru.krotarnya.diasync2.common.wear.WearSnapshot;

final class WearAlertController {
    private final WearAlertEvaluator evaluator;
    private final WearAlertStateStore stateStore;
    private final Consumer<AlertType> vibrator;
    private final Consumer<Instant> scheduler;
    private final Runnable cancelSchedule;

    static WearAlertController create(Context context) {
        Context applicationContext = context.getApplicationContext();
        WearAlertScheduler alertScheduler = new WearAlertScheduler();
        WearAlertVibrator alertVibrator = new WearAlertVibrator(applicationContext);
        return new WearAlertController(
                new WearAlertEvaluator(Clock.systemUTC()),
                new SharedPreferencesWearAlertStateStore(applicationContext),
                alertVibrator::vibrate,
                instant -> alertScheduler.schedule(applicationContext, instant),
                () -> alertScheduler.cancel(applicationContext));
    }

    WearAlertController(
            WearAlertEvaluator evaluator,
            WearAlertStateStore stateStore,
            Consumer<AlertType> vibrator,
            Consumer<Instant> scheduler,
            Runnable cancelSchedule
    ) {
        this.evaluator = Objects.requireNonNull(evaluator);
        this.stateStore = Objects.requireNonNull(stateStore);
        this.vibrator = Objects.requireNonNull(vibrator);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.cancelSchedule = Objects.requireNonNull(cancelSchedule);
    }

    boolean onSnapshot(WearSnapshot snapshot) {
        WearAlertEvaluation evaluation = evaluator.evaluate(snapshot, stateStore.read());
        boolean persisted = stateStore.write(evaluation.state());
        if (persisted && evaluation.vibration() != null) {
            vibrator.accept(evaluation.vibration());
        }
        if (evaluation.nextCheckAt() == null) {
            cancelSchedule.run();
        } else {
            scheduler.accept(evaluation.nextCheckAt());
        }
        return evaluation.complicationChanged();
    }
}
