package ru.krotarnya.diasync.common.events;

import java.util.List;

import lombok.Data;
import ru.krotarnya.diasync.common.repository.DataPoint;

@Data
public class NewData implements Event<NewData> {
    private final List<DataPoint> dataPoints;
}
