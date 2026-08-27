package ru.krotarnya.diasync2.data.api;

public final class BootstrapHttpException extends Exception {
    public BootstrapHttpException(int statusCode) {
        super("HTTP " + statusCode);
    }
}
