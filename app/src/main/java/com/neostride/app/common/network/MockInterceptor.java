package com.neostride.app.common.network;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.io.IOException;

public class MockInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        String uri = chain.request().url().uri().toString();

        if (uri.contains("records")) {
            // 웅천님의 getPaceColor 로직을 100% 만족시키는 다이나믹 데이터
            String json = "[\n" +
                    "  {\n" +
                    "    \"created_at\": \"2026-05-02T19:00:00\",\n" +
                    "    \"total_distance\": 4.2,\n" +
                    "    \"duration\": 1512.0,\n" +
                    "    \"pace\": 6.0,\n" +
                    "    \"calories\": 350.0,\n" +
                    "    \"segment_paces\": [4.2, 5.8, 6.8, 7.8, 9.2, 5.2],\n" + // 🔥 무지개색 핵심 (매우빠름 ~ 매우느림)
                    "    \"gps_traces\": [\n" +
                    "      {\"latitude\": 37.6180, \"longitude\": 126.7120, \"time\": \"2026-05-02T19:00:00\"},\n" + // 시작
                    "      {\"latitude\": 37.6210, \"longitude\": 126.7150, \"time\": \"2026-05-02T19:01:00\"},\n" + // 1구간 (초고속 - 초록)
                    "      {\"latitude\": 37.6230, \"longitude\": 126.7180, \"time\": \"2026-05-02T19:02:30\"},\n" + // 2구간 (빠름 - 연두)
                    "      {\"latitude\": 37.6250, \"longitude\": 126.7210, \"time\": \"2026-05-02T19:04:30\"},\n" + // 3구간 (보통 - 노랑)
                    "      {\"latitude\": 37.6240, \"longitude\": 126.7230, \"time\": \"2026-05-02T19:07:00\"},\n" + // 4구간 (느림 - 주황)
                    "      {\"latitude\": 37.6220, \"longitude\": 126.7240, \"time\": \"2026-05-02T19:10:30\"},\n" + // 5구간 (매우느림 - 빨강)
                    "      {\"latitude\": 37.6200, \"longitude\": 126.7220, \"time\": \"2026-05-02T19:12:00\"}\n"  + // 6구간 (다시 빠름 - 초록)
                    "    ]\n" +
                    "  }\n" +
                    "]";

            return new Response.Builder()
                    .code(200)
                    .message("OK")
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .body(ResponseBody.create(MediaType.parse("application/json"), json))
                    .addHeader("content-type", "application/json")
                    .build();
        }
        return chain.proceed(chain.request());
    }
}