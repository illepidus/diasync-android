package ru.krotarnya.diasync2.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.gson.Gson;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

public class HttpBootstrapDataSourceTest {
    @Test
    public void buildsBoundedGetRequestAndParsesResponse() throws Exception {
        AtomicReference<Request> captured = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    captured.set(chain.request());
                    return jsonResponse(chain.request(), 200, """
                            [{
                              "id": 7,
                              "userId": "demo",
                              "timestamp": "2026-08-27T11:59:00Z",
                              "sensorGlucose": {"mgdl": 123.0, "sensorId": "sensor"}
                            }]
                            """);
                })
                .build();
        HttpBootstrapDataSource source = new HttpBootstrapDataSource(client, new Gson());

        List<ApiDataPointDto> result = source.getDataPoints(
                "https://diasync.example",
                "demo",
                Instant.parse("2026-08-27T08:00:00Z"),
                Instant.parse("2026-08-27T12:00:00Z"));

        assertEquals(1, result.size());
        assertEquals(Double.valueOf(123.0), result.get(0).sensorGlucose.mgdl);
        Request request = captured.get();
        assertNotNull(request);
        assertEquals("GET", request.method());
        assertEquals("/api/v1/getDataPoints", request.url().encodedPath());
        assertEquals("demo", request.url().queryParameter("userId"));
        assertEquals("2026-08-27T08:00:00Z", request.url().queryParameter("from"));
        assertEquals("2026-08-27T12:00:00Z", request.url().queryParameter("to"));
    }

    @Test
    public void rejectsCleartextAndClassifiesHttpAndJsonFailures() {
        HttpBootstrapDataSource unusedNetwork = new HttpBootstrapDataSource(
                new OkHttpClient(),
                new Gson());
        assertThrows(IllegalArgumentException.class, () -> unusedNetwork.getDataPoints(
                "http://diasync.example",
                "demo",
                Instant.EPOCH,
                Instant.EPOCH));

        HttpBootstrapDataSource httpFailure = sourceReturning(503, "unavailable");
        HttpBootstrapExceptionResult httpResult = call(httpFailure);
        assertEquals(BootstrapHttpException.class, httpResult.exception.getClass());

        HttpBootstrapDataSource parseFailure = sourceReturning(200, "not-json");
        HttpBootstrapExceptionResult parseResult = call(parseFailure);
        assertEquals(BootstrapParseException.class, parseResult.exception.getClass());
    }

    private HttpBootstrapDataSource sourceReturning(int code, String body) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> jsonResponse(chain.request(), code, body))
                .build();
        return new HttpBootstrapDataSource(client, new Gson());
    }

    private HttpBootstrapExceptionResult call(HttpBootstrapDataSource source) {
        try {
            source.getDataPoints(
                    "https://diasync.example",
                    "demo",
                    Instant.EPOCH,
                    Instant.EPOCH);
            throw new AssertionError("Expected failure");
        } catch (IOException | BootstrapHttpException | BootstrapParseException exception) {
            return new HttpBootstrapExceptionResult(exception);
        }
    }

    private static Response jsonResponse(Request request, int code, String body) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(ResponseBody.create(body, MediaType.get("application/json")))
                .build();
    }

    private static final class HttpBootstrapExceptionResult {
        private final Exception exception;

        private HttpBootstrapExceptionResult(Exception exception) {
            this.exception = exception;
        }
    }
}
