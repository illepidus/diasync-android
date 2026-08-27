package ru.krotarnya.diasync2.data.api;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public interface BootstrapDataSource {
    List<ApiDataPointDto> getDataPoints(
            String baseUrl,
            String userId,
            Instant from,
            Instant to
    ) throws IOException, BootstrapHttpException, BootstrapParseException;
}
