package ru.krotarnya.diasync2.data.api;

public final class MasterUploadParseException extends Exception {
    public MasterUploadParseException(Throwable cause) {
        super("Backend upload response could not be parsed", cause);
    }
}
