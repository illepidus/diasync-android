package ru.krotarnya.diasync2.data.api;

import java.io.IOException;
import java.util.List;

public interface MasterUploadDataSource {
    List<ApiDataPointDto> addDataPoints(
            String baseUrl,
            List<ApiDataPointDto> points
    ) throws IOException, MasterUploadHttpException, MasterUploadParseException;

    void cancelActiveCall();
}
