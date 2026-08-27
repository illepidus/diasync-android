package ru.krotarnya.diasync2.data.api;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class HttpLongPollDataSource implements LongPollDataSource {
    public static final int SERVER_TIMEOUT_MILLIS = 75_000;
    private static final Type RESPONSE_TYPE = new TypeToken<List<ApiDataPointDto>>() { }.getType();

    private final OkHttpClient httpClient;
    private final Gson gson;

    public HttpLongPollDataSource(OkHttpClient httpClient, Gson gson) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.gson = Objects.requireNonNull(gson);
    }

    @Override
    public LongPollCall newCall(String baseUrl, String userId, Instant since) {
        HttpUrl parsedBase = HttpUrl.parse(baseUrl);
        if (parsedBase == null || !"https".equals(parsedBase.scheme())) {
            throw new IllegalArgumentException("Backend URL must use HTTPS");
        }
        HttpUrl url = parsedBase.newBuilder()
                .addPathSegments("api/v1/getDataPointsLongPoll")
                .addQueryParameter("userId", userId)
                .addQueryParameter("since", since.toString())
                .addQueryParameter("timeoutMs", Integer.toString(SERVER_TIMEOUT_MILLIS))
                .build();
        Call call = httpClient.newCall(new Request.Builder().url(url).get().build());
        return new RealLongPollCall(call, gson);
    }

    private static final class RealLongPollCall implements LongPollCall {
        private final Call call;
        private final Gson gson;

        private RealLongPollCall(Call call, Gson gson) {
            this.call = call;
            this.gson = gson;
        }

        @Override
        public List<ApiDataPointDto> execute()
                throws IOException, BootstrapHttpException, BootstrapParseException {
            try (Response response = call.execute()) {
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

        @Override
        public void cancel() {
            call.cancel();
        }
    }
}
