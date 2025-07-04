package ru.krotarnya.diasync.common.events;

import lombok.Data;

@Data
public class NoDataEvent implements Event<NoDataEvent> {
    private final String userId;
}
