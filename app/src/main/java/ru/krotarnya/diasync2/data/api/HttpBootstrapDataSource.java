package ru.krotarnya.diasync2.data.api;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class HttpBootstrapDataSource implements BootstrapDataSource {
    private static final Type RESPONSE_TYPE = new TypeToken<List<ApiDataPointDto>>() { }.getType();

    private final OkHttpClient httpClient;
    private final Gson gson;

    public HttpBootstrapDataSource(OkHttpClient httpClient, Gson gson) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.gson = Objects.requireNonNull(gson);
    }

    @Override
    public List<ApiDataPointDto> getDataPoints(
            String baseUrl,
            String userId,
            Instant from,
            Instant to
    ) throws IOException, BootstrapHttpException, BootstrapParseException {
        HttpUrl parsedBase = HttpUrl.parse(baseUrl);
        if (parsedBase == null || !"https".equals(parsedBase.scheme())) {
            throw new IllegalArgumentException("Backend URL must use HTTPS");
        }
        HttpUrl url = parsedBase.newBuilder()
                .addPathSegments("api/v1/getDataPoints")
                .addQueryParameter("userId", userId)
                .addQueryParameter("from", from.toString())
                .addQueryParameter("to", to.toString())
                .build();
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BootstrapHttpException(response.code());
            }
            ResponseBody body = response.body();
            try {
                List<ApiDataPointDto> points = gson.fromJson(body.charStream(), RESPONSE_TYPE);
                if (points == null) {
                    throw new JsonParseException("Expected array");
                }
                return points;
            } catch (JsonParseException | IllegalStateException exception) {
                throw new BootstrapParseException(exception);
            }
        }
    }
}
