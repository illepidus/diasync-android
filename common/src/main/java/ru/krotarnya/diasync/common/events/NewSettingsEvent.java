package ru.krotarnya.diasync.common.events;

import lombok.Data;
import ru.krotarnya.diasync.common.repository.Settings;

@Data
public class NewSettingsEvent implements Event<NewSettingsEvent> {
    private final Settings settings;
}
