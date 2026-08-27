package ru.krotarnya.diasync2.data.api;

import java.io.IOException;
import java.util.List;

public interface LongPollCall {
    List<ApiDataPointDto> execute()
            throws IOException, BootstrapHttpException, BootstrapParseException;

    void cancel();
}
