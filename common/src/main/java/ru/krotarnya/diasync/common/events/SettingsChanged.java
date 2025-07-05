package ru.krotarnya.diasync.common.events;

import lombok.Data;
import ru.krotarnya.diasync.common.repository.Settings;

@Data
public class SettingsChanged implements Event<SettingsChanged> {
    private final Settings settings;
}
