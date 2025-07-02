package ru.krotarnya.diasync.common.events;

public class NoDataEvent implements Event<NoDataEvent> {
    private final String userId;

    public NoDataEvent(String userId) {
        this.userId = userId;
    }

    public String userId() {
        return userId;
    }
}
