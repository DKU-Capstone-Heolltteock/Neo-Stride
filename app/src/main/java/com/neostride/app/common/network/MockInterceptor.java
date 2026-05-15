package com.neostride.app.common.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/*
 * 개발 중 실제 서버 대신 가짜 응답을 반환하는 Interceptor 클래스임
 * push/merge 전에는 사용하지 않도록 제거하거나 ApiClient로 되돌려야 함
 */
public class MockInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        String path = chain.request().url().encodedPath();

        if (path.equals("/feeds")) {
            return makeJsonResponse(chain, getMockFeedListJson());
        }

        return chain.proceed(chain.request());
    }

    /*
     * Mock JSON 응답을 OkHttp Response 형태로 만들어 반환함
     */
    private Response makeJsonResponse(Chain chain, String json) {
        return new Response.Builder()
                .code(200)
                .message("OK")
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .body(ResponseBody.create(
                        json,
                        MediaType.parse("application/json")
                ))
                .addHeader("content-type", "application/json")
                .build();
    }

    /*
     * GET /feeds 요청에 대해 반환할 가짜 피드 목록 JSON임
     * Swagger의 FeedResponse 구조와 필드명을 맞춤
     */
    private String getMockFeedListJson() {
        return "["
                + "{"
                + "\"feedId\":1,"
                + "\"profileImageUrl\":\"\","
                + "\"nickname\":\"mock_runner\","
                + "\"createdAt\":\"방금 전\","
                + "\"title\":\"목서버 피드 테스트\","
                + "\"content\":\"GET /feeds 연결 확인용 목데이터입니다.\","
                + "\"taggedCount\":2,"
                + "\"likeCount\":12,"
                + "\"commentCount\":3,"
                + "\"distance\":\"5.20 km\","
                + "\"duration\":\"00:32:10\","
                + "\"pace\":\"6'11\\\"\","
                + "\"mapVisible\":false,"
                + "\"routeMapImageUri\":\"\","
                + "\"imageUrls\":[]"
                + "},"
                + "{"
                + "\"feedId\":2,"
                + "\"profileImageUrl\":\"\","
                + "\"nickname\":\"neo_stride\","
                + "\"createdAt\":\"10분 전\","
                + "\"title\":\"오늘 러닝 완료\","
                + "\"content\":\"가볍게 3km 뛰고 왔습니다.\","
                + "\"taggedCount\":0,"
                + "\"likeCount\":8,"
                + "\"commentCount\":1,"
                + "\"distance\":\"3.00 km\","
                + "\"duration\":\"00:18:40\","
                + "\"pace\":\"6'13\\\"\","
                + "\"mapVisible\":false,"
                + "\"routeMapImageUri\":\"\","
                + "\"imageUrls\":[]"
                + "}"
                + "]";
    }
}