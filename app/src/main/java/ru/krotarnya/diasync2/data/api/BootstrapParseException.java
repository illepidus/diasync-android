package ru.krotarnya.diasync2.data.api;

public final class BootstrapParseException extends Exception {
    public BootstrapParseException(Throwable cause) {
        super("Invalid response", cause);
    }
}
