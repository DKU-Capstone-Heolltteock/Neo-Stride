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
        String method = chain.request().method();

        /*
         * 피드 목록 조회 Mock API임
         * GET /api/feeds 요청을 가로채서 가짜 피드 목록 JSON을 반환함
         */
        if (method.equals("GET") && path.equals("/api/feeds")) {
            return makeJsonResponse(chain, getMockFeedListJson());
        }

        /*
         * 피드 상세 조회 Mock API임
         * GET /api/feeds/{feedId} 요청을 가로채서 feedId에 맞는 가짜 상세 JSON을 반환함
         */
        if (method.equals("GET") && path.matches("/api/feeds/\\d+")) {
            Long feedId = extractFeedId(path);
            return makeJsonResponse(chain, getMockFeedDetailJson(feedId));
        }

        /*
         * 위에서 처리하지 않은 요청은 원래 네트워크 요청으로 넘김
         */
        return chain.proceed(chain.request());
    }

    /*
     * /api/feeds/{feedId} 형태의 path에서 feedId만 추출하는 함수임
     */
    private Long extractFeedId(String path) {
        try {
            String feedIdText = path.substring(path.lastIndexOf("/") + 1);
            return Long.parseLong(feedIdText);
        } catch (Exception e) {
            return 1L;
        }
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
     * GET /api/feeds 요청에 대해 반환할 가짜 피드 목록 JSON임
     * 목록 화면용 FeedResponse 구조와 필드명을 맞춤
     */
    private String getMockFeedListJson() {
        return "["
                + "{"
                + "\"feedId\":1,"
                + "\"profileImageUrl\":\"\","
                + "\"nickname\":\"mock_runner\","
                + "\"createdAt\":\"방금 전\","
                + "\"title\":\"목서버 피드 테스트\","
                + "\"content\":\"GET /api/feeds 연결 확인용 목데이터입니다.\","
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

    /*
     * GET /api/feeds/{feedId} 요청에 대해 반환할 가짜 피드 상세 JSON임
     * 상세 화면용 FeedDetailResponse 구조와 필드명을 맞춤
     */
    private String getMockFeedDetailJson(Long feedId) {
        if (feedId == 2L) {
            return "{"
                    + "\"feedId\":2,"
                    + "\"writerId\":102,"
                    + "\"profileImageUrl\":\"\","
                    + "\"nickname\":\"neo_stride\","
                    + "\"createdAt\":\"10분 전\","
                    + "\"title\":\"오늘 러닝 완료 - 상세\","
                    + "\"content\":\"가볍게 3km 뛰고 왔습니다.\\n상세 API인 GET /api/feeds/2 호출에 성공했습니다.\","
                    + "\"taggedCount\":0,"
                    + "\"likeCount\":8,"
                    + "\"commentCount\":1,"
                    + "\"liked\":true,"
                    + "\"bookmarked\":false,"
                    + "\"mine\":false,"
                    + "\"distance\":\"3.00 km\","
                    + "\"duration\":\"00:18:40\","
                    + "\"pace\":\"6'13\\\"\","
                    + "\"mapVisible\":false,"
                    + "\"routeMapImageUri\":\"\","
                    + "\"imageUrls\":[],"
                    + "\"comments\":["
                    + "{"
                    + "\"commentId\":201,"
                    + "\"writerId\":301,"
                    + "\"nickname\":\"runner_comment\","
                    + "\"profileImageUrl\":\"\","
                    + "\"content\":\"상세 댓글 목데이터입니다.\","
                    + "\"createdAt\":\"5분 전\","
                    + "\"mine\":false"
                    + "}"
                    + "]"
                    + "}";
        }

        return "{"
                + "\"feedId\":1,"
                + "\"writerId\":101,"
                + "\"profileImageUrl\":\"\","
                + "\"nickname\":\"mock_runner\","
                + "\"createdAt\":\"방금 전\","
                + "\"title\":\"목서버 피드 상세 테스트\","
                + "\"content\":\"GET /api/feeds/1 상세 조회 Mock 데이터입니다.\\n피드 상세 API 연결 확인용입니다.\","
                + "\"taggedCount\":2,"
                + "\"likeCount\":12,"
                + "\"commentCount\":3,"
                + "\"liked\":false,"
                + "\"bookmarked\":true,"
                + "\"mine\":true,"
                + "\"distance\":\"5.20 km\","
                + "\"duration\":\"00:32:10\","
                + "\"pace\":\"6'11\\\"\","
                + "\"mapVisible\":false,"
                + "\"routeMapImageUri\":\"\","
                + "\"imageUrls\":[],"
                + "\"comments\":["
                + "{"
                + "\"commentId\":101,"
                + "\"writerId\":201,"
                + "\"nickname\":\"comment_user1\","
                + "\"profileImageUrl\":\"\","
                + "\"content\":\"첫 번째 댓글입니다.\","
                + "\"createdAt\":\"1분 전\","
                + "\"mine\":false"
                + "},"
                + "{"
                + "\"commentId\":102,"
                + "\"writerId\":101,"
                + "\"nickname\":\"mock_runner\","
                + "\"profileImageUrl\":\"\","
                + "\"content\":\"내가 작성한 댓글 예시입니다.\","
                + "\"createdAt\":\"방금 전\","
                + "\"mine\":true"
                + "}"
                + "]"
                + "}";
    }
}