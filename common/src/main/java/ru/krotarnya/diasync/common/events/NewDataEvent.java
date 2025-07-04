package ru.krotarnya.diasync.common.events;

import java.util.List;

import lombok.Data;
import ru.krotarnya.diasync.common.repository.DataPoint;

@Data
public class NewDataEvent implements Event<NewDataEvent> {
    private final List<DataPoint> dataPoints;
}
