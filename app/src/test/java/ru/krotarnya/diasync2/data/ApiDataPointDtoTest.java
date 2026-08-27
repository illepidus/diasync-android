package ru.krotarnya.diasync2.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;
import java.time.Instant;
import org.junit.Test;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.data.api.ApiDataPointDto;
import ru.krotarnya.diasync2.data.local.DataPointEntity;

public class ApiDataPointDtoTest {
    @Test
    public void parsesAndMapsEveryBackendPointType() {
        String json = """
                {
                  "id": 42,
                  "userId": "secret-user",
                  "timestamp": "2026-08-27T10:00:00.123456789Z",
                  "updateTimestamp": "2026-08-27T10:00:01Z",
                  "sensorGlucose": {
                    "mgdl": 100.0,
                    "sensorId": "sensor-1",
                    "calibration": {"slope": 1.1, "intercept": 5.0}
                  },
                  "manualGlucose": {"mgdl": 105.0},
                  "carbs": {"grams": 12.5, "description": "snack"}
                }
                """;

        ApiDataPointDto dto = new Gson().fromJson(json, ApiDataPointDto.class);
        DataPointMapper mapper = new DataPointMapper();
        DataPointEntity entity = mapper.toEntity(dto, "secret-user");
        DataPoint domain = mapper.toDomain(entity);

        assertEquals(Long.valueOf(42), entity.serverId);
        assertEquals("2026-08-27T10:00:00.123456789Z", entity.timestamp);
        assertEquals(Double.valueOf(100.0), entity.sensorMgDl);
        assertEquals("sensor-1", entity.sensorId);
        assertEquals(Double.valueOf(1.1), entity.calibrationSlope);
        assertEquals(Double.valueOf(5.0), entity.calibrationIntercept);
        assertEquals(Double.valueOf(105.0), entity.manualMgDl);
        assertEquals(Double.valueOf(12.5), entity.carbsGrams);
        assertEquals("snack", entity.carbsDescription);
        assertNotNull(domain.sensorPoint());
        assertEquals(115.0, domain.sensorPoint().displayValue(true).mgDl(), 0.0001);
        assertEquals(Instant.parse("2026-08-27T10:00:01Z"), domain.updateTimestamp());
    }
}
