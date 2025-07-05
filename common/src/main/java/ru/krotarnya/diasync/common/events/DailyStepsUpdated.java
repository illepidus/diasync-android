package ru.krotarnya.diasync.common.events;

import lombok.Data;

@Data
public class DailyStepsUpdated implements Event<DailyStepsUpdated> {
    private final long steps;
}
