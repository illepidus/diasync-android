package ru.krotarnya.diasync2.common.wear;

public final class WearProtocolException extends IllegalArgumentException {
    public WearProtocolException(String message) {
        super(message);
    }

    public WearProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
