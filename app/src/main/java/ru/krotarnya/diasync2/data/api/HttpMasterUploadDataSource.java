package ru.krotarnya.diasync2.data.api;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class HttpMasterUploadDataSource implements MasterUploadDataSource {
    private static final Type RESPONSE_TYPE = new TypeToken<List<ApiDataPointDto>>() { }.getType();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final AtomicReference<Call> activeCall = new AtomicReference<>();

    public HttpMasterUploadDataSource(OkHttpClient httpClient, Gson gson) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.gson = Objects.requireNonNull(gson);
    }

    @Override
    public List<ApiDataPointDto> addDataPoints(
            String baseUrl,
            List<ApiDataPointDto> points
    ) throws IOException, MasterUploadHttpException, MasterUploadParseException {
        HttpUrl parsedBase = HttpUrl.parse(baseUrl);
        if (parsedBase == null || !"https".equals(parsedBase.scheme())) {
            throw new IllegalArgumentException("Backend URL must use HTTPS");
        }
        HttpUrl url = parsedBase.newBuilder().addPathSegments("api/v1/addDataPoints").build();
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(gson.toJson(points), JSON))
                .build();
        Call call = httpClient.newCall(request);
        if (!activeCall.compareAndSet(null, call)) {
            throw new IllegalStateException("Another master upload call is active");
        }
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new MasterUploadHttpException(response.code());
            }
            ResponseBody body = response.body();
            try {
                List<ApiDataPointDto> result = gson.fromJson(body.charStream(), RESPONSE_TYPE);
                if (result == null) {
                    throw new JsonParseException("Expected array");
                }
                return result;
            } catch (JsonParseException | IllegalStateException exception) {
                throw new MasterUploadParseException(exception);
            }
        } finally {
            activeCall.compareAndSet(call, null);
        }
    }

    @Override
    public void cancelActiveCall() {
        Call call = activeCall.get();
        if (call != null) {
            call.cancel();
        }
    }
}
