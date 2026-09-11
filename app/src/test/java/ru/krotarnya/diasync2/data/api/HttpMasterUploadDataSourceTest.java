package ru.krotarnya.diasync2.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.gson.Gson;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

public class HttpMasterUploadDataSourceTest {
    @Test
    public void postsJsonBatchAndParsesAcknowledgements() throws Exception {
        AtomicReference<Request> captured = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    captured.set(chain.request());
                    return response(chain.request(), 200, """
                            [{"id":7,"userId":"secret","timestamp":"2026-09-10T12:00:00Z",
                            "updateTimestamp":"2026-09-10T12:01:00Z",
                            "manualGlucose":{"mgdl":121.0}}]
                            """);
                })
                .build();
        HttpMasterUploadDataSource source = new HttpMasterUploadDataSource(client, new Gson());
        ApiDataPointDto point = new ApiDataPointDto();
        point.userId = "secret";
        point.timestamp = "2026-09-10T12:00:00Z";
        point.manualGlucose = new ApiDataPointDto.ManualGlucoseDto();
        point.manualGlucose.mgdl = 121.0;

        List<ApiDataPointDto> result = source.addDataPoints(
                "https://diasync.example/",
                List.of(point));

        assertEquals(Long.valueOf(7), result.get(0).id);
        assertNotNull(captured.get());
        assertEquals("POST", captured.get().method());
        assertEquals("/api/v1/addDataPoints", captured.get().url().encodedPath());
    }

    @Test
    public void rejectsCleartextAndClassifiesFailures() {
        HttpMasterUploadDataSource source = new HttpMasterUploadDataSource(
                new OkHttpClient(),
                new Gson());
        assertThrows(IllegalArgumentException.class, () -> source.addDataPoints(
                "http://diasync.example/",
                List.of()));

        HttpMasterUploadDataSource httpFailure = returning(413, "too large");
        MasterUploadHttpException exception = assertThrows(
                MasterUploadHttpException.class,
                () -> httpFailure.addDataPoints("https://diasync.example/", List.of()));
        assertEquals(413, exception.statusCode());

        HttpMasterUploadDataSource parseFailure = returning(200, "not-json");
        assertThrows(MasterUploadParseException.class, () -> parseFailure.addDataPoints(
                "https://diasync.example/",
                List.of()));
    }

    private static HttpMasterUploadDataSource returning(int code, String body) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> response(chain.request(), code, body))
                .build();
        return new HttpMasterUploadDataSource(client, new Gson());
    }

    private static Response response(Request request, int code, String body) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(ResponseBody.create(body, MediaType.get("application/json")))
                .build();
    }
}
