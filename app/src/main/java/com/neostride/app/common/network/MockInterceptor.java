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

    // 업로드된 피드 Mock 데이터를 임시 저장함
    private static String uploadedFeedJson = null;

    @Override
    public Response intercept(Chain chain) throws IOException {
        String path = chain.request().url().encodedPath();
        String method = chain.request().method();

        if (method.equals("GET") && path.equals("/api/community/feeds")) {
            return makeJsonResponse(chain, getMockFeedListJson());
        }

        if (method.equals("GET") && path.matches("/api/community/feeds/\\d+")) {
            Long feedId = extractFeedId(path);
            return makeJsonResponse(chain, getMockFeedDetailJson(feedId));
        }

        if (method.equals("POST") && path.equals("/api/community/feeds")) {
            uploadedFeedJson = getMockUploadFeedJson();
            return makeJsonResponse(chain, uploadedFeedJson);
        }

        /*
         * 친구 목록 조회 Mock API임
         * GET /community/friends 요청을 가로채서 가짜 친구 목록을 반환함
         */
        if (method.equals("GET") && path.equals("/api/community/friends")) {
            return makeJsonResponse(chain, getMockFriendListJson());
        }

        return chain.proceed(chain.request());
    }

    private Long extractFeedId(String path) {
        try {
            String feedIdText = path.substring(path.lastIndexOf("/") + 1);
            return Long.parseLong(feedIdText);
        } catch (Exception e) {
            return 1L;
        }
    }

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
     * 업로드된 Mock 피드가 있으면 목록 맨 위에 추가함
     */
    private String getMockFeedListJson() {
        String defaultFeeds =
                "{"
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
                        + "}";

        if (uploadedFeedJson != null) {
            return "[" + uploadedFeedJson + "," + defaultFeeds + "]";
        }

        return "[" + defaultFeeds + "]";
    }

    private String getMockFeedDetailJson(Long feedId) {
        if (feedId == 999L && uploadedFeedJson != null) {
            return "{"
                    + "\"feedId\":999,"
                    + "\"writerId\":999,"
                    + "\"profileImageUrl\":\"\","
                    + "\"nickname\":\"mock_runner\","
                    + "\"createdAt\":\"방금 전\","
                    + "\"title\":\"업로드 성공 테스트\","
                    + "\"content\":\"Mock 업로드 응답 데이터입니다.\","
                    + "\"taggedCount\":0,"
                    + "\"likeCount\":0,"
                    + "\"commentCount\":0,"
                    + "\"liked\":false,"
                    + "\"bookmarked\":false,"
                    + "\"mine\":true,"
                    + "\"distance\":\"0.04 km\","
                    + "\"duration\":\"00:10\","
                    + "\"pace\":\"4:10/km\","
                    + "\"mapVisible\":true,"
                    + "\"routeMapImageUri\":\"\","
                    + "\"imageUrls\":[],"
                    + "\"comments\":[]"
                    + "}";
        }

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
                    + "\"comments\":[]"
                    + "}";
        }

        return "{"
                + "\"feedId\":1,"
                + "\"writerId\":101,"
                + "\"profileImageUrl\":\"\","
                + "\"nickname\":\"mock_runner\","
                + "\"createdAt\":\"방금 전\","
                + "\"title\":\"목서버 피드 상세 테스트\","
                + "\"content\":\"GET /api/feeds/1 상세 조회 Mock 데이터입니다.\","
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
                + "\"comments\":[]"
                + "}";
    }

    /*
     * POST /api/feeds 요청에 대해 반환할 가짜 업로드 성공 JSON임
     */
    private String getMockUploadFeedJson() {
        return "{"
                + "\"feedId\":999,"
                + "\"profileImageUrl\":\"\","
                + "\"nickname\":\"mock_runner\","
                + "\"createdAt\":\"방금 전\","
                + "\"title\":\"업로드 성공 테스트\","
                + "\"content\":\"Mock 업로드 응답 데이터입니다.\","
                + "\"taggedCount\":0,"
                + "\"likeCount\":0,"
                + "\"commentCount\":0,"
                + "\"distance\":\"0.04 km\","
                + "\"duration\":\"00:10\","
                + "\"pace\":\"4:10/km\","
                + "\"mapVisible\":true,"
                + "\"routeMapImageUri\":\"\","
                + "\"imageUrls\":[]"
                + "}";
    }

    /*
     * 친구 목록 Mock JSON 데이터임
     * 사람 태그 다이얼로그 테스트용 데이터임
     */
    private String getMockFriendListJson() {
        return "["
                + "{"
                + "\"user_id\":1,"
                + "\"nickname\":\"neo_runner\","
                + "\"badge_tier\":\"GOLD\","
                + "\"friend_count\":120,"
                + "\"profile_image_url\":\"\","
                + "\"status\":\"ACCEPTED\""
                + "},"
                + "{"
                + "\"user_id\":2,"
                + "\"nickname\":\"marathon_kim\","
                + "\"badge_tier\":\"SILVER\","
                + "\"friend_count\":87,"
                + "\"profile_image_url\":\"\","
                + "\"status\":\"ACCEPTED\""
                + "},"
                + "{"
                + "\"user_id\":3,"
                + "\"nickname\":\"night_runner\","
                + "\"badge_tier\":\"BRONZE\","
                + "\"friend_count\":42,"
                + "\"profile_image_url\":\"\","
                + "\"status\":\"ACCEPTED\""
                + "}"
                + "]";
    }
}