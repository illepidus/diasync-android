package ru.krotarnya.diasync.common.events;

import java.util.List;

import ru.krotarnya.diasync.common.repository.DataPoint;

public class NewDataEvent implements Event<NewDataEvent> {
    private final List<DataPoint> dataPoints;

    public NewDataEvent(List<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public List<DataPoint> dataPoints() {
        return dataPoints;
    }
}
