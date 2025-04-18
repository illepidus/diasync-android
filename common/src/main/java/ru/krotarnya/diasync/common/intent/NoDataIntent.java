package ru.krotarnya.diasync.common.intent;

import android.content.Intent;

import java.util.Optional;

public class NoDataIntent {
    private static final String ACTION = "ru.krotarnya.diasync.NO_DATA";
    private static final String USER_ID_EXTRA_FIELD = "userId";
    private final String userId;

    private NoDataIntent(String userId) {
        this.userId = userId;
    }

    public static Intent build(String userId) {
        return new Intent(ACTION).putExtra(USER_ID_EXTRA_FIELD, userId);
    }

    public static Optional<NoDataIntent> parse(Intent intent) {
        if (!ACTION.equals(intent.getAction())) {
            return Optional.empty();
        }
        return Optional.ofNullable(intent.getStringExtra(USER_ID_EXTRA_FIELD)).map(NoDataIntent::new);
    }

    public String userId() {
        return userId;
    }
}
