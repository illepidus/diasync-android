package ru.krotarnya.diasync.common.service;

import android.os.BatteryManager;

import java.util.List;
import java.util.stream.Collectors;

import ru.krotarnya.diasync.common.model.BatteryStatus;
import ru.krotarnya.diasync.common.model.WidgetData;
import ru.krotarnya.diasync.common.repository.DataPoint;
import ru.krotarnya.diasync.common.repository.Database;

public class WatchDataProvider implements WidgetDataProvider {
    private final Database db;
    private final BatteryManager batteryManager;


    public WatchDataProvider(Database db, BatteryManager batteryManager) {
        this.db = db;
        this.batteryManager = batteryManager;
    }

    @Override
    public WidgetData get() {
        List<DataPoint> dataPoints = db.dataPointDao()
                .findLast("krotarino")
                .stream()
                .collect(Collectors.toList());

        return WidgetData.builder()
                .dataPoints(dataPoints)
                .batteryStatus(new BatteryStatus(batteryManager))
                .build();
    }
}
