package ru.krotarnya.diasync.common.service;

import java.util.List;
import java.util.stream.Collectors;

import ru.krotarnya.diasync.common.model.WidgetData;
import ru.krotarnya.diasync.common.repository.DataPoint;
import ru.krotarnya.diasync.common.repository.Database;

public class WidgetDataProviderImpl implements WidgetDataProvider {
    private final Database db;

    public WidgetDataProviderImpl(Database db) {
        this.db = db;
    }

    @Override
    public WidgetData get() {
        List<DataPoint> dataPoints = db.dataPointDao()
                .findLast("krotarino")
                .stream()
                .collect(Collectors.toList());

        return WidgetData.builder()
                .dataPoints(dataPoints)
                .build();
    }
}
