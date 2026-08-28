package ru.krotarnya.diasync2.common;

import java.util.Objects;
import java.util.Optional;

public final class AlertDecision {
    private static final AlertDecision NONE = new AlertDecision(null);

    private final AlertType type;

    private AlertDecision(AlertType type) {
        this.type = type;
    }

    public static AlertDecision none() {
        return NONE;
    }

    public static AlertDecision alert(AlertType type) {
        return new AlertDecision(Objects.requireNonNull(type));
    }

    public boolean shouldAlert() {
        return type != null;
    }

    public Optional<AlertType> type() {
        return Optional.ofNullable(type);
    }
}
