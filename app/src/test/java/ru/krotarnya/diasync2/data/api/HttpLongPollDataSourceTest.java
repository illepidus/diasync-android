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

public class HttpLongPollDataSourceTest {
    @Test
    public void buildsLongPollRequestAndParsesImmediateData() throws Exception {
        AtomicReference<Request> captured = new AtomicReference<>();
        HttpLongPollDataSource source = source((request) -> {
            captured.set(request);
            return jsonResponse(request, 200, """
                    [{
                      "id": 7,
                      "userId": "demo",
                      "timestamp": "2026-08-27T11:59:00Z",
                      "updateTimestamp": "2026-08-27T12:00:01Z",
                      "sensorGlucose": {"mgdl": 123.0, "sensorId": "sensor"}
                    }]
                    """);
        });

        List<ApiDataPointDto> result = source.newCall(
                "https://diasync.example",
                "demo",
                Instant.parse("2026-08-27T12:00:00Z")).execute();

        assertEquals(1, result.size());
        Request request = captured.get();
        assertNotNull(request);
        assertEquals("/api/v1/getDataPointsLongPoll", request.url().encodedPath());
        assertEquals("demo", request.url().queryParameter("userId"));
        assertEquals("2026-08-27T12:00:00Z", request.url().queryParameter("since"));
        assertEquals("75000", request.url().queryParameter("timeoutMs"));
    }

    @Test
    public void acceptsEmptyTimeoutAndClassifiesHttpMalformedAndDisconnect() throws Exception {
        assertEquals(0, source(request -> jsonResponse(request, 200, "[]"))
                .newCall("https://diasync.example", "demo", Instant.EPOCH)
                .execute().size());

        assertThrows(BootstrapHttpException.class, () -> source(
                request -> jsonResponse(request, 503, "unavailable"))
                .newCall("https://diasync.example", "demo", Instant.EPOCH)
                .execute());
        assertThrows(BootstrapParseException.class, () -> source(
                request -> jsonResponse(request, 200, "not-json"))
                .newCall("https://diasync.example", "demo", Instant.EPOCH)
                .execute());
        assertThrows(IOException.class, () -> source(request -> {
            throw new IOException("disconnect");
        }).newCall("https://diasync.example", "demo", Instant.EPOCH).execute());
    }

    @Test
    public void rejectsCleartextBeforeCreatingCall() {
        HttpLongPollDataSource source = new HttpLongPollDataSource(new OkHttpClient(), new Gson());

        assertThrows(IllegalArgumentException.class, () -> source.newCall(
                "http://diasync.example", "demo", Instant.EPOCH));
    }

    private HttpLongPollDataSource source(ResponseFactory factory) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> factory.create(chain.request()))
                .build();
        return new HttpLongPollDataSource(client, new Gson());
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

    private interface ResponseFactory {
        Response create(Request request) throws IOException;
    }
}
