package ru.krotarnya.diasync.common.intent;

import android.content.Intent;
import android.content.IntentFilter;

import java.util.Optional;

public class NewDataIntent {
    private static final String ACTION = "ru.krotarnya.diasync.NEW_DATA";
    private static final String USER_ID_EXTRA_FIELD = "userId";
    private final String userId;

    private NewDataIntent(String userId) {
        this.userId = userId;
    }

    public static Intent build(String userId) {
        return new Intent(ACTION).putExtra(USER_ID_EXTRA_FIELD, userId);
    }

    public static Optional<NewDataIntent> parse(Intent intent) {
        if (!ACTION.equals(intent.getAction())) {
            return Optional.empty();
        }
        return Optional.ofNullable(intent.getStringExtra(USER_ID_EXTRA_FIELD)).map(NewDataIntent::new);
    }

    public static IntentFilter filter() {
        return new IntentFilter(ACTION);
    }

    public String userId() {
        return userId;
    }
}
