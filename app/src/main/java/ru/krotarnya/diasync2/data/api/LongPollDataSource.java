package ru.krotarnya.diasync2.data.api;

import java.time.Instant;

public interface LongPollDataSource {
    LongPollCall newCall(String baseUrl, String userId, Instant since);
}
