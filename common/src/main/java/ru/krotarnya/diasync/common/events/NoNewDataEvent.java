package ru.krotarnya.diasync.common.events;

import lombok.Data;

@Data
public class NoNewDataEvent implements Event<NoNewDataEvent> {
    private final String userId;
}
