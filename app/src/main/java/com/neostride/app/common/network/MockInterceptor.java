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

        /*
         * 팁 목록 조회 Mock API임
         * GET /api/community/tips 요청을 가로채서 가짜 팁 목록을 반환함
         */
        if (method.equals("GET") && path.equals("/api/community/tips")) {
            return makeJsonResponse(chain, getMockTipListJson());
        }

        /*
         * 팁 업로드 Mock API임
         * POST /api/community/tips 요청을 가로채서 가짜 업로드 성공 응답을 반환함
         */
        if (method.equals("POST") && path.equals("/api/community/tips")) {
            return makeJsonResponse(chain, getMockUploadTipJson());
        }

        /*
         * 팁 상세 조회 Mock API임
         * GET /api/community/tips/{tipId} 요청을 가로채서 가짜 팁 상세 정보를 반환함
         */
        if (method.equals("GET") && path.matches("/api/community/tips/\\d+")) {
            Long tipId = extractTipId(path);
            return makeJsonResponse(chain, getMockTipDetailJson(tipId));
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

    /*
     * 요청 경로에서 tipId를 추출하는 함수임
     * 예: /api/community/tips/3 -> 3
     */
    private Long extractTipId(String path) {
        try {
            String tipIdText = path.substring(path.lastIndexOf("/") + 1);
            return Long.parseLong(tipIdText);
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

    /*
     * GET /api/community/tips/{tipId} 요청에 대해 반환할 가짜 팁 상세 JSON임
     */
    private String getMockTipDetailJson(Long tipId) {
        if (tipId == 2L) {
            return "{"
                    + "\"tipId\":2,"
                    + "\"writerId\":102,"
                    + "\"nickname\":\"neo_stride\","
                    + "\"profileImageUrl\":\"\","
                    + "\"badgeOwned\":true,"
                    + "\"badgeType\":\"SILVER\","
                    + "\"category\":\"COURSE\","
                    + "\"title\":\"야간 러닝 코스 추천\","
                    + "\"content\":\"가로등이 많고 사람이 적당히 있는 코스를 선택하는 것이 좋습니다.\\n너무 어두운 길은 피하는 것이 안전합니다.\\n상세 API Mock 호출 성공 데이터입니다.\","
                    + "\"gpsVisible\":true,"
                    + "\"routeMapImageUrl\":\"\","
                    + "\"imageUrls\":[],"
                    + "\"likeCount\":8,"
                    + "\"commentCount\":2,"
                    + "\"liked\":true,"
                    + "\"bookmarked\":false,"
                    + "\"mine\":false,"
                    + "\"createdAt\":\"10분 전\","
                    + "\"comments\":["
                    + "{"
                    + "\"commentId\":201,"
                    + "\"writerId\":301,"
                    + "\"nickname\":\"night_runner\","
                    + "\"profileImageUrl\":\"\","
                    + "\"content\":\"저도 야간에는 밝은 길 위주로 뛰는 게 좋더라고요.\","
                    + "\"createdAt\":\"5분 전\","
                    + "\"mine\":false"
                    + "},"
                    + "{"
                    + "\"commentId\":202,"
                    + "\"writerId\":302,"
                    + "\"nickname\":\"course_master\","
                    + "\"profileImageUrl\":\"\","
                    + "\"content\":\"코스 추천 감사합니다!\","
                    + "\"createdAt\":\"2분 전\","
                    + "\"mine\":false"
                    + "}"
                    + "]"
                    + "}";
        }

        if (tipId == 3L) {
            return "{"
                    + "\"tipId\":3,"
                    + "\"writerId\":103,"
                    + "\"nickname\":\"gear_master\","
                    + "\"profileImageUrl\":\"\","
                    + "\"badgeOwned\":false,"
                    + "\"badgeType\":\"NONE\","
                    + "\"category\":\"GEAR\","
                    + "\"title\":\"러닝화는 쿠션보다 발에 맞는지가 먼저입니다\","
                    + "\"content\":\"처음 러닝화를 고를 때는 브랜드보다 발볼, 착화감, 통증 여부를 먼저 확인하는 것이 좋습니다.\\n발에 맞지 않는 신발은 장거리 러닝에서 통증을 만들 수 있습니다.\","
                    + "\"gpsVisible\":false,"
                    + "\"routeMapImageUrl\":\"\","
                    + "\"imageUrls\":[],"
                    + "\"likeCount\":5,"
                    + "\"commentCount\":0,"
                    + "\"liked\":false,"
                    + "\"bookmarked\":true,"
                    + "\"mine\":false,"
                    + "\"createdAt\":\"1시간 전\","
                    + "\"comments\":[]"
                    + "}";
        }

        return "{"
                + "\"tipId\":1,"
                + "\"writerId\":101,"
                + "\"nickname\":\"mock_tip_runner\","
                + "\"profileImageUrl\":\"\","
                + "\"badgeOwned\":true,"
                + "\"badgeType\":\"GOLD\","
                + "\"category\":\"TRAINING\","
                + "\"title\":\"제발!!!!!천천히 뛰세요!\","
                + "\"content\":\"처음에는 속도보다 꾸준함이 중요합니다.\\n5분 뛰고 2분 걷는 방식으로 시작하면 부상 위험을 줄일 수 있습니다.\\n처음부터 빠르게 뛰면 쉽게 지치고 무릎이나 발목에 부담이 갈 수 있습니다.\","
                + "\"gpsVisible\":false,"
                + "\"routeMapImageUrl\":\"\","
                + "\"imageUrls\":[],"
                + "\"likeCount\":12,"
                + "\"commentCount\":3,"
                + "\"liked\":false,"
                + "\"bookmarked\":true,"
                + "\"mine\":true,"
                + "\"createdAt\":\"방금 전\","
                + "\"comments\":["
                + "{"
                + "\"commentId\":101,"
                + "\"writerId\":201,"
                + "\"nickname\":\"beginner_runner\","
                + "\"profileImageUrl\":\"\","
                + "\"content\":\"초보자인데 도움 됐습니다!\","
                + "\"createdAt\":\"3분 전\","
                + "\"mine\":false"
                + "},"
                + "{"
                + "\"commentId\":102,"
                + "\"writerId\":101,"
                + "\"nickname\":\"mock_tip_runner\","
                + "\"profileImageUrl\":\"\","
                + "\"content\":\"천천히 늘려가면 훨씬 오래 뛸 수 있어요.\","
                + "\"createdAt\":\"1분 전\","
                + "\"mine\":true"
                + "}"
                + "]"
                + "}";
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

    /*
     * GET /api/community/tips 요청에 대해 반환할 가짜 팁 목록 JSON임
     * 피드 목록과 동일하게 배열 형태로 반환함
     */
    private String getMockTipListJson() {
        return "["
                + "{"
                + "\"tipId\":1,"
                + "\"nickname\":\"mock_tip_runner\","
                + "\"profileImageUrl\":\"\","
                + "\"badgeOwned\":true,"
                + "\"badgeType\":\"GOLD\","
                + "\"category\":\"TRAINING\","
                + "\"title\":\"제발!!!!!천천히 뛰세요!\","
                + "\"content\":\"처음에는 속도보다 꾸준함이 중요합니다. 5분 뛰고 2분 걷는 방식으로 시작하면 부상 위험을 줄일 수 있습니다.\","
                + "\"gpsVisible\":false,"
                + "\"routeMapImageUrl\":\"\","
                + "\"imageUrls\":[],"
                + "\"likeCount\":12,"
                + "\"commentCount\":3,"
                + "\"liked\":false,"
                + "\"bookmarked\":false,"
                + "\"createdAt\":\"방금 전\""
                + "},"
                + "{"
                + "\"tipId\":2,"
                + "\"nickname\":\"neo_stride\","
                + "\"profileImageUrl\":\"\","
                + "\"badgeOwned\":true,"
                + "\"badgeType\":\"SILVER\","
                + "\"category\":\"COURSE\","
                + "\"title\":\"야간 러닝 코스 추천\","
                + "\"content\":\"가로등이 많고 사람이 적당히 있는 코스를 선택하는 것이 좋습니다. 너무 어두운 길은 피하는 것이 안전합니다.\","
                + "\"gpsVisible\":true,"
                + "\"routeMapImageUrl\":\"\","
                + "\"imageUrls\":[],"
                + "\"likeCount\":8,"
                + "\"commentCount\":1,"
                + "\"liked\":true,"
                + "\"bookmarked\":false,"
                + "\"createdAt\":\"10분 전\""
                + "},"
                + "{"
                + "\"tipId\":3,"
                + "\"nickname\":\"gear_master\","
                + "\"profileImageUrl\":\"\","
                + "\"badgeOwned\":false,"
                + "\"badgeType\":\"NONE\","
                + "\"category\":\"GEAR\","
                + "\"title\":\"러닝화는 쿠션보다 발에 맞는지가 먼저입니다\","
                + "\"content\":\"처음 러닝화를 고를 때는 브랜드보다 발볼, 착화감, 통증 여부를 먼저 확인하는 것이 좋습니다.\","
                + "\"gpsVisible\":false,"
                + "\"routeMapImageUrl\":\"\","
                + "\"imageUrls\":[],"
                + "\"likeCount\":5,"
                + "\"commentCount\":0,"
                + "\"liked\":false,"
                + "\"bookmarked\":true,"
                + "\"createdAt\":\"1시간 전\""
                + "}"
                + "]";
    }

    /*
     * POST /api/community/tips 요청에 대해 반환할 가짜 팁 업로드 성공 JSON임
     */
    private String getMockUploadTipJson() {
        return "{"
                + "\"tipId\":999,"
                + "\"nickname\":\"mock_tip_runner\","
                + "\"profileImageUrl\":\"\","
                + "\"badgeOwned\":true,"
                + "\"category\":\"자유\","
                + "\"title\":\"팁 업로드 성공 테스트\","
                + "\"content\":\"Mock 팁 업로드 응답 데이터입니다.\","
                + "\"gpsVisible\":false,"
                + "\"routeMapImageUrl\":\"\","
                + "\"imageUrls\":[],"
                + "\"likeCount\":0,"
                + "\"commentCount\":0,"
                + "\"createdAt\":\"방금 전\""
                + "}";
    }
}