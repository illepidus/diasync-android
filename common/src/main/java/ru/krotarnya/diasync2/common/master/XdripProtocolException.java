package ru.krotarnya.diasync2.common.master;

public final class XdripProtocolException extends IllegalArgumentException {
    public XdripProtocolException(String message) {
        super(message);
    }

    public XdripProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
