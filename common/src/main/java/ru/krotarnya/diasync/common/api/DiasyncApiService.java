package ru.krotarnya.diasync.common.api;

import java.time.Instant;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.krotarnya.diasync.common.model.DataPoint;

public interface DiasyncApiService {
    @GET("/api/v1/getDataPoints")
    Call<List<DataPoint>> getDataPoints(
            @Query("userId") String userId,
            @Query("from") Instant fromInclusive,
            @Query("to") Instant toExclusive);
}