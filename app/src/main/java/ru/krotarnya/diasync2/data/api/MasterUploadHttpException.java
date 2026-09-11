package ru.krotarnya.diasync2.data.api;

public final class MasterUploadHttpException extends Exception {
    private final int statusCode;

    public MasterUploadHttpException(int statusCode) {
        super("Backend upload failed with HTTP " + statusCode);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
