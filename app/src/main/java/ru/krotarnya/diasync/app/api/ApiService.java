package ru.krotarnya.diasync.app.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.krotarnya.diasync.app.model.DataPoint;

public interface ApiService {
    @GET("/api/v1/getDataPoints")
    Call<List<DataPoint>> getDataPoints(@Query("userId") String userId);
}