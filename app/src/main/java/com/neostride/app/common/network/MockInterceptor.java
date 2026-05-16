package com.neostride.app.common.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    /*
     * 피드 좋아요 상태를 목서버 내부에서 임시 저장하는 Map임
     * key는 feedId, value는 좋아요 여부임
     */
    private static final Map<Long, Boolean> mockFeedLikeStateMap = new HashMap<>();

    /*
     * 피드 북마크 상태를 목서버 내부에서 임시 저장하는 Map임
     * key는 feedId, value는 북마크 여부임
     */
    private static final Map<Long, Boolean> mockFeedBookmarkStateMap = new HashMap<>();

    /*
     * 피드 좋아요 개수를 목서버 내부에서 임시 저장하는 Map임
     * key는 feedId, value는 좋아요 개수임
     */
    private static final Map<Long, Integer> mockFeedLikeCountMap = new HashMap<>();

    /*
     * 팁 좋아요 상태를 목서버 내부에서 임시 저장하는 Map임
     * key는 tipId, value는 좋아요 여부임
     */
    private static final Map<Long, Boolean> mockTipLikeStateMap = new HashMap<>();

    /*
     * 팁 북마크 상태를 목서버 내부에서 임시 저장하는 Map임
     * key는 tipId, value는 북마크 여부임
     */
    private static final Map<Long, Boolean> mockTipBookmarkStateMap = new HashMap<>();

    /*
     * 팁 좋아요 개수를 목서버 내부에서 임시 저장하는 Map임
     * key는 tipId, value는 좋아요 개수임
     */
    private static final Map<Long, Integer> mockTipLikeCountMap = new HashMap<>();

    @Override
    public Response intercept(Chain chain) throws IOException {
        String path = chain.request().url().encodedPath();
        String method = chain.request().method();

        /*
         * 피드 검색 Mock API임
         * GET /api/community/search/feeds 요청을 가로채서 피드 목록 형식의 검색 결과를 반환함
         */
        if (method.equals("GET") && path.equals("/api/community/search/feeds")) {
            String keyword = chain.request().url().queryParameter("keyword");
            return makeJsonResponse(chain, getMockSearchFeedListJson(keyword));
        }

        /*
         * 팁 검색 Mock API임
         * GET /api/community/search/tips 요청을 가로채서 팁 목록 형식의 검색 결과를 반환함
         */
        if (method.equals("GET") && path.equals("/api/community/search/tips")) {
            String keyword = chain.request().url().queryParameter("keyword");
            String category = chain.request().url().queryParameter("category");

            return makeJsonResponse(chain, getMockSearchTipListJson(keyword, category));
        }

        /*
         * 프로필 검색 Mock API임
         * GET /api/community/search/profiles 요청을 가로채서 유저 검색 결과를 반환함
         */
        if (method.equals("GET") && path.equals("/api/community/search/profiles")) {
            String keyword = chain.request().url().queryParameter("keyword");
            return makeJsonResponse(chain, getMockSearchProfileListJson(keyword));
        }

        /*
         * 친구 검색 Mock API임
         * GET /api/community/search/friends 요청을 가로채서 친구 검색 결과를 반환함
         */
        if (method.equals("GET") && path.equals("/api/community/search/friends")) {
            String keyword = chain.request().url().queryParameter("keyword");
            return makeJsonResponse(chain, getMockSearchFriendListJson(keyword));
        }


        /*
         * 피드 목록 조회 Mock API임
         * GET /api/community/feeds 요청을 가로채서 가짜 피드 목록을 반환함
         */
        if (method.equals("GET") && path.equals("/api/community/feeds")) {
            return makeJsonResponse(chain, getMockFeedListJson());
        }

        /*
         * 마이페이지 프로필 조회 Mock API임
         * GET /users/me/profile 요청을 가로채서 내 프로필 정보를 반환함
         */
        if (method.equals("GET") && path.equals("/users/me/profile")) {
            return makeJsonResponse(chain, getMockMyProfileJson());
        }

        /*
         * 마이페이지 상태 메시지 수정 Mock API임
         * PATCH /users/me/status 요청을 가로채서 성공 응답을 반환함
         */
        if (method.equals("PATCH") && path.equals("/users/me/status")) {
            return makeJsonResponse(chain, "{}");
        }

        /*
         * 마이페이지 프로필 이미지 수정 Mock API임
         * PATCH /users/me/profile-image 요청을 가로채서 성공 응답을 반환함
         */
        if (method.equals("PATCH") && path.equals("/users/me/profile-image")) {
            return makeJsonResponse(chain, "{}");
        }

        /*
         * 러너페이지 프로필 조회 Mock API임
         * GET /users/{userId}/profile 요청을 가로채서 특정 사용자 프로필 정보를 반환함
         */
        if (method.equals("GET") && path.matches("/users/\\d+/profile")) {
            int userId = extractUserId(path);
            return makeJsonResponse(chain, getMockRunnerProfileJson(userId));
        }

        /*
         * 내 뱃지 조회 Mock API임
         * GET /users/me/badge 요청을 가로채서 내 뱃지 정보를 반환함
         */
        if (method.equals("GET") && path.equals("/users/me/badge")) {
            return makeJsonResponse(chain, getMockBadgeJson(101));
        }

        /*
         * 러너 뱃지 조회 Mock API임
         * GET /users/{userId}/badge 요청을 가로채서 특정 사용자 뱃지 정보를 반환함
         */
        if (method.equals("GET") && path.matches("/users/\\d+/badge")) {
            int userId = extractUserId(path);
            return makeJsonResponse(chain, getMockBadgeJson(userId));
        }

        /*
         * 내 피드 목록 조회 Mock API임
         * GET /community/contents/me 요청을 가로채서 내 피드 목록을 반환함
         */
        if (method.equals("GET") && path.equals("/community/contents/me")) {
            return makeJsonResponse(chain, getMockCommunityContentListJson(101));
        }

        /*
         * 나를 태그한 피드 목록 조회 Mock API임
         * GET /community/contents/tagged 요청을 가로채서 태그된 피드 목록을 반환함
         */
        if (method.equals("GET") && path.equals("/community/contents/tagged")) {
            return makeJsonResponse(chain, getMockCommunityContentListJson(102));
        }

        /*
         * 내가 댓글 단 피드 목록 조회 Mock API임
         * GET /community/contents/comments 요청을 가로채서 댓글 단 피드 목록을 반환함
         */
        if (method.equals("GET") && path.equals("/community/contents/comments")) {
            return makeJsonResponse(chain, getMockCommunityContentListJson(103));
        }

        /*
         * 내가 좋아요 한 피드 목록 조회 Mock API임
         * GET /community/contents/likes 요청을 가로채서 좋아요한 피드 목록을 반환함
         */
        if (method.equals("GET") && path.equals("/community/contents/likes")) {
            return makeJsonResponse(chain, getMockCommunityContentListJson(102));
        }

        /*
         * 내가 북마크 한 피드 목록 조회 Mock API임
         * GET /community/contents/bookmarks 요청을 가로채서 북마크한 피드 목록을 반환함
         */
        if (method.equals("GET") && path.equals("/community/contents/bookmarks")) {
            return makeJsonResponse(chain, getMockCommunityContentListJson(103));
        }

        /*
         * 러너페이지 작성 피드 목록 조회 Mock API임
         * GET /community/contents/user/{userId} 요청을 가로채서 해당 유저의 피드 목록을 반환함
         */
        if (method.equals("GET") && path.matches("/community/contents/user/\\d+")) {
            int userId = extractLastInt(path);
            return makeJsonResponse(chain, getMockCommunityContentListJson(userId));
        }

        /*
         * 마이페이지 북마크 토글 Mock API임
         * POST /community/bookmark/{contentId} 요청을 가로채서 성공 응답을 반환함
         */
        if (method.equals("POST") && path.matches("/community/bookmark/\\d+")) {
            return makeJsonResponse(chain, "{}");
        }

        /*
         * 계정 정보 조회 Mock API임
         * GET /users/me/account 요청을 가로채서 계정 정보를 반환함
         */
        if (method.equals("GET") && path.equals("/users/me/account")) {
            return makeJsonResponse(chain, getMockAccountJson());
        }

        /*
         * 닉네임 변경 Mock API임
         * PATCH /users/me/nickname 요청을 가로채서 성공 응답을 반환함
         */
        if (method.equals("PATCH") && path.equals("/users/me/nickname")) {
            return makeJsonResponse(chain, "{}");
        }

        /*
         * 계정 탈퇴 Mock API임
         * DELETE /users/me 요청을 가로채서 성공 응답을 반환함
         */
        if (method.equals("DELETE") && path.equals("/users/me")) {
            return makeJsonResponse(chain, "{}");
        }

        /*
         * 피드 상세 조회 Mock API임
         * GET /api/community/feeds/{feedId} 요청을 가로채서 가짜 피드 상세를 반환함
         */
        if (method.equals("GET") && path.matches("/api/community/feeds/\\d+")) {
            Long feedId = extractFeedId(path);
            return makeJsonResponse(chain, getMockFeedDetailJson(feedId));
        }

        /*
         * 피드 업로드 Mock API임
         * POST /api/community/feeds 요청을 가로채서 가짜 업로드 성공 응답을 반환함
         */
        if (method.equals("POST") && path.equals("/api/community/feeds")) {
            uploadedFeedJson = getMockUploadFeedJson();
            return makeJsonResponse(chain, uploadedFeedJson);
        }

        /*
         * 피드 좋아요 토글 Mock API임
         * POST /api/community/feeds/{feedId}/likes 요청을 가로채서 가짜 좋아요 응답을 반환함
         */
        if (method.equals("POST") && path.matches("/api/community/feeds/\\d+/likes")) {
            Long feedId = extractFeedIdFromActionPath(path);
            return makeJsonResponse(chain, getMockFeedLikeJson(feedId));
        }

        /*
         * 피드 북마크 토글 Mock API임
         * POST /api/community/feeds/{feedId}/bookmarks 요청을 가로채서 가짜 북마크 응답을 반환함
         */
        if (method.equals("POST") && path.matches("/api/community/feeds/\\d+/bookmarks")) {
            Long feedId = extractFeedIdFromActionPath(path);
            return makeJsonResponse(chain, getMockFeedBookmarkJson(feedId));
        }

        /*
         * 피드 댓글 작성 Mock API임
         * POST /api/community/feeds/{feedId}/comments 요청을 가로채서 가짜 댓글 작성 응답을 반환함
         */
        if (method.equals("POST") && path.matches("/api/community/feeds/\\d+/comments")) {
            Long feedId = extractFeedIdFromActionPath(path);
            return makeJsonResponse(chain, getMockCreateFeedCommentJson(feedId));
        }

        /*
         * 친구 목록 조회 Mock API임
         * GET /api/community/friends 요청을 가로채서 가짜 친구 목록을 반환함
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
         * 팁 좋아요 토글 Mock API임
         * POST /api/community/tips/{tipId}/likes 요청을 가로채서 가짜 좋아요 응답을 반환함
         */
        if (method.equals("POST") && path.matches("/api/community/tips/\\d+/likes")) {
            Long tipId = extractTipIdFromActionPath(path);
            return makeJsonResponse(chain, getMockTipLikeJson(tipId));
        }

        /*
         * 팁 북마크 토글 Mock API임
         * POST /api/community/tips/{tipId}/bookmarks 요청을 가로채서 가짜 북마크 응답을 반환함
         */
        if (method.equals("POST") && path.matches("/api/community/tips/\\d+/bookmarks")) {
            Long tipId = extractTipIdFromActionPath(path);
            return makeJsonResponse(chain, getMockTipBookmarkJson(tipId));
        }

        /*
         * 팁 댓글 작성 Mock API임
         * POST /api/community/tips/{tipId}/comments 요청을 가로채서 가짜 댓글 작성 응답을 반환함
         */
        if (method.equals("POST") && path.matches("/api/community/tips/\\d+/comments")) {
            Long tipId = extractTipIdFromActionPath(path);
            return makeJsonResponse(chain, getMockCreateTipCommentJson(tipId));
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

    /*
     * 요청 경로에서 feedId를 추출하는 함수임
     * 예: /api/community/feeds/1 -> 1
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

    /*
     * 좋아요/북마크 같은 피드 액션 API 경로에서 feedId를 추출하는 함수임
     * 예: /api/community/feeds/1/likes -> 1
     */
    private Long extractFeedIdFromActionPath(String path) {
        try {
            String[] parts = path.split("/");
            return Long.parseLong(parts[4]);
        } catch (Exception e) {
            return 1L;
        }
    }

    /*
     * 좋아요/북마크/댓글 작성 같은 액션 API 경로에서 tipId를 추출하는 함수임
     * 예: /api/community/tips/3/likes -> 3
     */
    private Long extractTipIdFromActionPath(String path) {
        try {
            String[] parts = path.split("/");
            return Long.parseLong(parts[4]);
        } catch (Exception e) {
            return 1L;
        }
    }

    /*
     * JSON 문자열을 OkHttp Response 객체로 만들어 반환하는 함수임
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
     * feedId별 기본 좋아요 상태를 반환하는 함수임
     */
    private boolean getDefaultFeedLiked(Long feedId) {
        if (feedId == 2L) {
            return true;
        }

        return false;
    }

    /*
     * feedId별 기본 북마크 상태를 반환하는 함수임
     */
    private boolean getDefaultFeedBookmarked(Long feedId) {
        if (feedId == 1L) {
            return true;
        }

        return false;
    }

    /*
     * feedId별 기본 좋아요 개수를 반환하는 함수임
     */
    private int getDefaultFeedLikeCount(Long feedId) {
        if (feedId == 2L) {
            return 8;
        }

        if (feedId == 999L) {
            return 0;
        }

        return 12;
    }

    /*
     * 현재 목서버에 저장된 피드 좋아요 상태를 반환하는 함수임
     * 아직 토글된 적이 없으면 기본값을 반환함
     */
    private boolean getCurrentFeedLiked(Long feedId) {
        if (mockFeedLikeStateMap.containsKey(feedId)) {
            return mockFeedLikeStateMap.get(feedId);
        }

        return getDefaultFeedLiked(feedId);
    }

    /*
     * 현재 목서버에 저장된 피드 북마크 상태를 반환하는 함수임
     * 아직 토글된 적이 없으면 기본값을 반환함
     */
    private boolean getCurrentFeedBookmarked(Long feedId) {
        if (mockFeedBookmarkStateMap.containsKey(feedId)) {
            return mockFeedBookmarkStateMap.get(feedId);
        }

        return getDefaultFeedBookmarked(feedId);
    }

    /*
     * 현재 목서버에 저장된 피드 좋아요 개수를 반환하는 함수임
     * 아직 토글된 적이 없으면 기본값을 반환함
     */
    private int getCurrentFeedLikeCount(Long feedId) {
        if (mockFeedLikeCountMap.containsKey(feedId)) {
            return mockFeedLikeCountMap.get(feedId);
        }

        return getDefaultFeedLikeCount(feedId);
    }

    /*
     * tipId별 기본 좋아요 상태를 반환하는 함수임
     */
    private boolean getDefaultTipLiked(Long tipId) {
        if (tipId == 2L) {
            return true;
        }

        return false;
    }

    /*
     * tipId별 기본 북마크 상태를 반환하는 함수임
     */
    private boolean getDefaultTipBookmarked(Long tipId) {
        if (tipId == 1L || tipId == 3L) {
            return true;
        }

        return false;
    }

    /*
     * tipId별 기본 좋아요 개수를 반환하는 함수임
     */
    private int getDefaultTipLikeCount(Long tipId) {
        if (tipId == 2L) {
            return 8;
        }

        if (tipId == 3L) {
            return 5;
        }

        return 12;
    }

    /*
     * 현재 목서버에 저장된 팁 좋아요 상태를 반환하는 함수임
     * 아직 토글된 적이 없으면 기본값을 반환함
     */
    private boolean getCurrentTipLiked(Long tipId) {
        if (mockTipLikeStateMap.containsKey(tipId)) {
            return mockTipLikeStateMap.get(tipId);
        }

        return getDefaultTipLiked(tipId);
    }

    /*
     * 현재 목서버에 저장된 팁 북마크 상태를 반환하는 함수임
     * 아직 토글된 적이 없으면 기본값을 반환함
     */
    private boolean getCurrentTipBookmarked(Long tipId) {
        if (mockTipBookmarkStateMap.containsKey(tipId)) {
            return mockTipBookmarkStateMap.get(tipId);
        }

        return getDefaultTipBookmarked(tipId);
    }

    /*
     * 현재 목서버에 저장된 팁 좋아요 개수를 반환하는 함수임
     * 아직 토글된 적이 없으면 기본값을 반환함
     */
    private int getCurrentTipLikeCount(Long tipId) {
        if (mockTipLikeCountMap.containsKey(tipId)) {
            return mockTipLikeCountMap.get(tipId);
        }

        return getDefaultTipLikeCount(tipId);
    }

    /*
     * GET /api/community/feeds 요청에 대해 반환할 가짜 피드 목록 JSON임
     * 업로드된 Mock 피드가 있으면 목록 맨 위에 추가함
     */
    private String getMockFeedListJson() {
        String defaultFeeds =
                "{"
                        + "\"feedId\":1,"
                        + "\"writerId\":101,"
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
                        + "\"writerId\":102,"
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
     * 좋아요/북마크 상태는 목서버 Map에 저장된 최신 상태를 반영함
     */
    private String getMockTipDetailJson(Long tipId) {
        boolean liked = getCurrentTipLiked(tipId);
        boolean bookmarked = getCurrentTipBookmarked(tipId);
        int likeCount = getCurrentTipLikeCount(tipId);

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
                    + "\"likeCount\":" + likeCount + ","
                    + "\"commentCount\":2,"
                    + "\"liked\":" + liked + ","
                    + "\"bookmarked\":" + bookmarked + ","
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
                    + "\"likeCount\":" + likeCount + ","
                    + "\"commentCount\":0,"
                    + "\"liked\":" + liked + ","
                    + "\"bookmarked\":" + bookmarked + ","
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
                + "\"likeCount\":" + likeCount + ","
                + "\"commentCount\":3,"
                + "\"liked\":" + liked + ","
                + "\"bookmarked\":" + bookmarked + ","
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

    /*
     * GET /api/community/feeds/{feedId} 요청에 대해 반환할 가짜 피드 상세 JSON임
     * 좋아요/북마크 상태는 목서버 Map에 저장된 최신 상태를 반영함
     */
    private String getMockFeedDetailJson(Long feedId) {
        boolean liked = getCurrentFeedLiked(feedId);
        boolean bookmarked = getCurrentFeedBookmarked(feedId);
        int likeCount = getCurrentFeedLikeCount(feedId);

        if (feedId == 999L && uploadedFeedJson != null) {
            return "{"
                    + "\"feedId\":999,"
                    + "\"writerId\":101,"
                    + "\"profileImageUrl\":\"\","
                    + "\"nickname\":\"mock_runner\","
                    + "\"createdAt\":\"방금 전\","
                    + "\"title\":\"업로드 성공 테스트\","
                    + "\"content\":\"Mock 업로드 응답 데이터입니다.\","
                    + "\"taggedCount\":0,"
                    + "\"likeCount\":" + likeCount + ","
                    + "\"commentCount\":0,"
                    + "\"liked\":" + liked + ","
                    + "\"bookmarked\":" + bookmarked + ","
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
                    + "\"likeCount\":" + likeCount + ","
                    + "\"commentCount\":1,"
                    + "\"liked\":" + liked + ","
                    + "\"bookmarked\":" + bookmarked + ","
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
                    + "\"nickname\":\"night_runner\","
                    + "\"profileImageUrl\":\"\","
                    + "\"content\":\"오늘 페이스 좋네요! 저도 저녁에 뛰어야겠습니다.\","
                    + "\"createdAt\":\"4분 전\","
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
                + "\"content\":\"GET /api/feeds/1 상세 조회 Mock 데이터입니다.\\n댓글 카드 UI와 점3개 메뉴 테스트용 데이터도 함께 내려줍니다.\","
                + "\"taggedCount\":2,"
                + "\"likeCount\":" + likeCount + ","
                + "\"commentCount\":3,"
                + "\"liked\":" + liked + ","
                + "\"bookmarked\":" + bookmarked + ","
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
                + "\"nickname\":\"Ongcheon1004\","
                + "\"profileImageUrl\":\"\","
                + "\"content\":\"헬떡헬떡..\","
                + "\"createdAt\":\"4분 전\","
                + "\"mine\":false"
                + "},"
                + "{"
                + "\"commentId\":102,"
                + "\"writerId\":202,"
                + "\"nickname\":\"YoonHyeon7942\","
                + "\"profileImageUrl\":\"\","
                + "\"content\":\"헬떡헬떡22..\","
                + "\"createdAt\":\"2분 전\","
                + "\"mine\":false"
                + "},"
                + "{"
                + "\"commentId\":103,"
                + "\"writerId\":101,"
                + "\"nickname\":\"mock_runner\","
                + "\"profileImageUrl\":\"\","
                + "\"content\":\"이건 내가 쓴 댓글 테스트입니다. 점3개 누르면 수정/삭제가 떠야 합니다.\","
                + "\"createdAt\":\"방금 전\","
                + "\"mine\":true"
                + "}"
                + "]"
                + "}";
    }

    /*
     * POST /api/community/feeds 요청에 대해 반환할 가짜 업로드 성공 JSON임
     */
    private String getMockUploadFeedJson() {
        return "{"
                + "\"feedId\":999,"
                + "\"writerId\":101,"
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
     * 현재 목서버 Map에 저장된 좋아요/북마크 상태를 목록에도 반영함
     */
    private String getMockTipListJson() {
        boolean tip1Liked = getCurrentTipLiked(1L);
        boolean tip1Bookmarked = getCurrentTipBookmarked(1L);
        int tip1LikeCount = getCurrentTipLikeCount(1L);

        boolean tip2Liked = getCurrentTipLiked(2L);
        boolean tip2Bookmarked = getCurrentTipBookmarked(2L);
        int tip2LikeCount = getCurrentTipLikeCount(2L);

        boolean tip3Liked = getCurrentTipLiked(3L);
        boolean tip3Bookmarked = getCurrentTipBookmarked(3L);
        int tip3LikeCount = getCurrentTipLikeCount(3L);

        return "["
                + "{"
                + "\"tipId\":1,"
                + "\"writerId\":101,"
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
                + "\"likeCount\":" + tip1LikeCount + ","
                + "\"commentCount\":3,"
                + "\"liked\":" + tip1Liked + ","
                + "\"bookmarked\":" + tip1Bookmarked + ","
                + "\"createdAt\":\"방금 전\""
                + "},"
                + "{"
                + "\"tipId\":2,"
                + "\"writerId\":102,"
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
                + "\"likeCount\":" + tip2LikeCount + ","
                + "\"commentCount\":1,"
                + "\"liked\":" + tip2Liked + ","
                + "\"bookmarked\":" + tip2Bookmarked + ","
                + "\"createdAt\":\"10분 전\""
                + "},"
                + "{"
                + "\"tipId\":3,"
                + "\"writerId\":103,"
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
                + "\"likeCount\":" + tip3LikeCount + ","
                + "\"commentCount\":0,"
                + "\"liked\":" + tip3Liked + ","
                + "\"bookmarked\":" + tip3Bookmarked + ","
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
                + "\"writerId\":101,"
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

    /*
     * POST /api/community/tips/{tipId}/likes 요청에 대해 반환할 가짜 좋아요 응답 JSON임
     * 같은 버튼을 다시 누르면 좋아요가 취소되도록 목서버 내부 상태를 토글함
     */
    private String getMockTipLikeJson(Long tipId) {
        boolean currentLiked = getCurrentTipLiked(tipId);
        int currentLikeCount = getCurrentTipLikeCount(tipId);

        boolean nextLiked = !currentLiked;

        if (nextLiked) {
            currentLikeCount++;
        } else {
            currentLikeCount = Math.max(0, currentLikeCount - 1);
        }

        mockTipLikeStateMap.put(tipId, nextLiked);
        mockTipLikeCountMap.put(tipId, currentLikeCount);

        return "{"
                + "\"tipId\":" + tipId + ","
                + "\"liked\":" + nextLiked + ","
                + "\"likeCount\":" + currentLikeCount
                + "}";
    }

    /*
     * POST /api/community/tips/{tipId}/bookmarks 요청에 대해 반환할 가짜 북마크 응답 JSON임
     * 같은 버튼을 다시 누르면 북마크가 취소되도록 목서버 내부 상태를 토글함
     */
    private String getMockTipBookmarkJson(Long tipId) {
        boolean currentBookmarked = getCurrentTipBookmarked(tipId);
        boolean nextBookmarked = !currentBookmarked;

        mockTipBookmarkStateMap.put(tipId, nextBookmarked);

        return "{"
                + "\"tipId\":" + tipId + ","
                + "\"bookmarked\":" + nextBookmarked
                + "}";
    }

    /*
     * POST /api/community/tips/{tipId}/comments 요청에 대해 반환할 가짜 댓글 작성 응답 JSON임
     */
    private String getMockCreateTipCommentJson(Long tipId) {
        return "{"
                + "\"commentId\":999,"
                + "\"writerId\":999,"
                + "\"nickname\":\"mock_tip_runner\","
                + "\"profileImageUrl\":\"\","
                + "\"content\":\"목서버 댓글 작성 성공\","
                + "\"createdAt\":\"방금 전\","
                + "\"mine\":true"
                + "}";
    }

    /*
     * POST /api/community/feeds/{feedId}/likes 요청에 대해 반환할 가짜 좋아요 응답 JSON임
     * 같은 버튼을 다시 누르면 좋아요가 취소되도록 목서버 내부 상태를 토글함
     */
    private String getMockFeedLikeJson(Long feedId) {
        boolean currentLiked = getCurrentFeedLiked(feedId);
        int currentLikeCount = getCurrentFeedLikeCount(feedId);

        boolean nextLiked = !currentLiked;

        if (nextLiked) {
            currentLikeCount++;
        } else {
            currentLikeCount = Math.max(0, currentLikeCount - 1);
        }

        mockFeedLikeStateMap.put(feedId, nextLiked);
        mockFeedLikeCountMap.put(feedId, currentLikeCount);

        return "{"
                + "\"feedId\":" + feedId + ","
                + "\"liked\":" + nextLiked + ","
                + "\"likeCount\":" + currentLikeCount
                + "}";
    }

    /*
     * POST /api/community/feeds/{feedId}/bookmarks 요청에 대해 반환할 가짜 북마크 응답 JSON임
     * 같은 버튼을 다시 누르면 북마크가 취소되도록 목서버 내부 상태를 토글함
     */
    private String getMockFeedBookmarkJson(Long feedId) {
        boolean currentBookmarked = getCurrentFeedBookmarked(feedId);
        boolean nextBookmarked = !currentBookmarked;

        mockFeedBookmarkStateMap.put(feedId, nextBookmarked);

        return "{"
                + "\"feedId\":" + feedId + ","
                + "\"bookmarked\":" + nextBookmarked
                + "}";
    }

    /*
     * POST /api/community/feeds/{feedId}/comments 요청에 대해 반환할 가짜 댓글 작성 응답 JSON임
     */
    private String getMockCreateFeedCommentJson(Long feedId) {
        return "{"
                + "\"commentId\":999,"
                + "\"writerId\":999,"
                + "\"nickname\":\"mock_runner\","
                + "\"profileImageUrl\":\"\","
                + "\"content\":\"목서버 피드 댓글 작성 성공\","
                + "\"createdAt\":\"방금 전\","
                + "\"mine\":true"
                + "}";
    }

    /*
     * /users/{userId}/profile, /users/{userId}/badge 같은 경로에서 userId를 추출하는 함수임
     * 예: /users/102/profile -> 102
     */
    private int extractUserId(String path) {
        try {
            String[] parts = path.split("/");
            return Integer.parseInt(parts[2]);
        } catch (Exception e) {
            return 101;
        }
    }

    /*
     * 경로의 마지막 숫자 값을 추출하는 함수임
     * 예: /community/contents/user/102 -> 102
     */
    private int extractLastInt(String path) {
        try {
            String text = path.substring(path.lastIndexOf("/") + 1);
            return Integer.parseInt(text);
        } catch (Exception e) {
            return 101;
        }
    }

    /*
     * 내 마이페이지 프로필 Mock JSON 데이터임
     */
    private String getMockMyProfileJson() {
        return "{"
                + "\"community_profile_name\":\"mock_runner\","
                + "\"profile_photo\":\"\","
                + "\"status_message\":\"오늘도 네온처럼 달리는 중\","
                + "\"friend_count\":12,"
                + "\"post_count\":3,"
                + "\"tagged_count\":2,"
                + "\"commented_feed_count\":4,"
                + "\"liked_feed_count\":5,"
                + "\"bookmarked_feed_count\":2"
                + "}";
    }

    /*
     * 러너페이지 프로필 Mock JSON 데이터임
     * userId에 따라 닉네임과 상태 메시지를 다르게 반환함
     */
    private String getMockRunnerProfileJson(int userId) {
        String nickname;
        String statusMessage;
        int friendCount;
        int postCount;
        boolean isFriend;

        if (userId == 102) {
            nickname = "neo_stride";
            statusMessage = "저녁 러닝 좋아합니다.";
            friendCount = 87;
            postCount = 5;
            isFriend = true;
        } else if (userId == 103) {
            nickname = "gear_master";
            statusMessage = "러닝화랑 장비 리뷰 좋아함";
            friendCount = 42;
            postCount = 2;
            isFriend = false;
        } else if (userId == 999) {
            nickname = "mock_runner";
            statusMessage = "방금 업로드한 목 유저입니다.";
            friendCount = 12;
            postCount = 3;
            isFriend = true;
        } else {
            nickname = "mock_runner_" + userId;
            statusMessage = "러닝을 즐기는 목 유저입니다.";
            friendCount = 20;
            postCount = 3;
            isFriend = false;
        }

        return "{"
                + "\"community_profile_name\":\"" + nickname + "\","
                + "\"profile_photo\":\"\","
                + "\"status_message\":\"" + statusMessage + "\","
                + "\"friend_count\":" + friendCount + ","
                + "\"post_count\":" + postCount + ","
                + "\"is_friend\":" + isFriend
                + "}";
    }

    /*
     * 뱃지 상세 Mock JSON 데이터임
     * userId에 따라 배지 등급을 다르게 반환함
     */
    private String getMockBadgeJson(int userId) {
        String tier;

        if (userId == 101) {
            tier = "GOLD";
        } else if (userId == 102) {
            tier = "SILVER";
        } else if (userId == 103) {
            tier = "BRONZE";
        } else {
            tier = "NONE";
        }

        return "{"
                + "\"Badge\":\"" + tier + "\","
                + "\"record_id\":1,"
                + "\"distance\":5.2,"
                + "\"pace\":\"6'11\\\"\","
                + "\"achieved_at\":\"2026-05-16\""
                + "}";
    }

    /*
     * 마이페이지/러너페이지 피드 목록에서 사용하는 CommunityContentResponse 배열 Mock JSON 데이터임
     */
    private String getMockCommunityContentListJson(int userId) {
        String nickname;
        String badgeTier;

        if (userId == 102) {
            nickname = "neo_stride";
            badgeTier = "SILVER";
        } else if (userId == 103) {
            nickname = "gear_master";
            badgeTier = "BRONZE";
        } else if (userId == 999) {
            nickname = "mock_runner";
            badgeTier = "GOLD";
        } else {
            nickname = "mock_runner";
            badgeTier = "GOLD";
        }

        return "["
                + "{"
                + "\"content_id\":1,"
                + "\"user_id\":" + userId + ","
                + "\"profile_image_url\":\"\","
                + "\"content_title\":\"목서버 피드 테스트\","
                + "\"content_text\":\"마이페이지/러너페이지 연결 확인용 목데이터입니다.\","
                + "\"nickname\":\"" + nickname + "\","
                + "\"total_distance\":5.2,"
                + "\"duration\":1930,"
                + "\"pace\":371,"
                + "\"created_at\":\"방금 전\","
                + "\"image_url\":\"\","
                + "\"tag_count\":2,"
                + "\"like_count\":12,"
                + "\"comment_count\":3,"
                + "\"is_bookmarked\":true,"
                + "\"is_liked\":false,"
                + "\"is_commented\":true,"
                + "\"is_tagged\":false,"
                + "\"badge_tier\":\"" + badgeTier + "\","
                + "\"route_map_url\":\"\""
                + "},"
                + "{"
                + "\"content_id\":2,"
                + "\"user_id\":102,"
                + "\"profile_image_url\":\"\","
                + "\"content_title\":\"오늘 러닝 완료\","
                + "\"content_text\":\"가볍게 3km 뛰고 왔습니다.\","
                + "\"nickname\":\"neo_stride\","
                + "\"total_distance\":3.0,"
                + "\"duration\":1120,"
                + "\"pace\":373,"
                + "\"created_at\":\"10분 전\","
                + "\"image_url\":\"\","
                + "\"tag_count\":0,"
                + "\"like_count\":8,"
                + "\"comment_count\":1,"
                + "\"is_bookmarked\":false,"
                + "\"is_liked\":true,"
                + "\"is_commented\":false,"
                + "\"is_tagged\":true,"
                + "\"badge_tier\":\"SILVER\","
                + "\"route_map_url\":\"\""
                + "}"
                + "]";
    }

    /*
     * GET /api/community/search/feeds 요청에 대해 반환할 피드 검색 Mock JSON 데이터임
     * 기존 피드 목록에서 사용하는 FeedResponse 형식과 동일하게 반환함
     */
    private String getMockSearchFeedListJson(String keyword) {
        String safeKeyword = getSafeSearchKeyword(keyword);

        StringBuilder builder = new StringBuilder();
        builder.append("[");

        boolean hasItem = false;

        hasItem = appendSearchFeedItem(
                builder,
                hasItem,
                1L,
                101L,
                "mock_runner",
                "방금 전",
                "목서버 피드 테스트",
                "GET /api/feeds 연결 확인용 목데이터입니다.",
                2,
                12,
                3,
                "5.20 km",
                "00:32:10",
                "6'11\\\"",
                false,
                "",
                safeKeyword
        );

        hasItem = appendSearchFeedItem(
                builder,
                hasItem,
                2L,
                102L,
                "neo_stride",
                "10분 전",
                "오늘 러닝 완료",
                "가볍게 3km 뛰고 왔습니다.",
                0,
                8,
                1,
                "3.00 km",
                "00:18:40",
                "6'13\\\"",
                false,
                "",
                safeKeyword
        );

        hasItem = appendSearchFeedItem(
                builder,
                hasItem,
                999L,
                101L,
                "mock_runner",
                "방금 전",
                "업로드 성공 테스트",
                "Mock 업로드 응답 데이터입니다.",
                0,
                0,
                0,
                "0.04 km",
                "00:10",
                "4:10/km",
                true,
                "",
                safeKeyword
        );

        builder.append("]");
        return builder.toString();
    }

    /*
     * 피드 검색 결과 JSON 배열에 피드 1개를 조건부로 추가하는 함수임
     */
    private boolean appendSearchFeedItem(
            StringBuilder builder,
            boolean hasItem,
            Long feedId,
            Long writerId,
            String nickname,
            String createdAt,
            String title,
            String content,
            int taggedCount,
            int likeCount,
            int commentCount,
            String distance,
            String duration,
            String pace,
            boolean mapVisible,
            String routeMapImageUri,
            String keyword
    ) {
        if (!matchesSearchKeyword(title, content, nickname, keyword)) {
            return hasItem;
        }

        if (hasItem) {
            builder.append(",");
        }

        builder.append("{")
                .append("\"feedId\":").append(feedId).append(",")
                .append("\"writerId\":").append(writerId).append(",")
                .append("\"profileImageUrl\":\"\",")
                .append("\"nickname\":\"").append(escapeJson(nickname)).append("\",")
                .append("\"createdAt\":\"").append(escapeJson(createdAt)).append("\",")
                .append("\"title\":\"").append(escapeJson(title)).append("\",")
                .append("\"content\":\"").append(escapeJson(content)).append("\",")
                .append("\"taggedCount\":").append(taggedCount).append(",")
                .append("\"likeCount\":").append(likeCount).append(",")
                .append("\"commentCount\":").append(commentCount).append(",")
                .append("\"distance\":\"").append(escapeJson(distance)).append("\",")
                .append("\"duration\":\"").append(escapeJson(duration)).append("\",")
                .append("\"pace\":\"").append(escapeJson(pace)).append("\",")
                .append("\"mapVisible\":").append(mapVisible).append(",")
                .append("\"routeMapImageUri\":\"").append(escapeJson(routeMapImageUri)).append("\",")
                .append("\"imageUrls\":[]")
                .append("}");

        return true;
    }

    /*
     * GET /api/community/search/tips 요청에 대해 반환할 팁 검색 Mock JSON 데이터임
     * 기존 팁 목록에서 사용하는 TipResponse 형식과 동일하게 반환함
     */
    private String getMockSearchTipListJson(String keyword, String category) {
        String safeKeyword = getSafeSearchKeyword(keyword);
        String safeCategory = category == null || category.trim().isEmpty()
                ? "ALL"
                : category.trim().toUpperCase();

        StringBuilder builder = new StringBuilder();
        builder.append("[");

        boolean hasItem = false;

        if (safeCategory.equals("ALL") || safeCategory.equals("TRAINING")) {
            hasItem = appendSearchTipItem(
                    builder,
                    hasItem,
                    1L,
                    101L,
                    "mock_tip_runner",
                    true,
                    "GOLD",
                    "TRAINING",
                    "제발!!!!!천천히 뛰세요!",
                    "처음에는 속도보다 꾸준함이 중요합니다. 5분 뛰고 2분 걷는 방식으로 시작하면 부상 위험을 줄일 수 있습니다.",
                    false,
                    12,
                    3,
                    false,
                    true,
                    "방금 전",
                    safeKeyword
            );
        }

        if (safeCategory.equals("ALL") || safeCategory.equals("COURSE")) {
            hasItem = appendSearchTipItem(
                    builder,
                    hasItem,
                    2L,
                    102L,
                    "neo_stride",
                    true,
                    "SILVER",
                    "COURSE",
                    "야간 러닝 코스 추천",
                    "가로등이 많고 사람이 적당히 있는 코스를 선택하는 것이 좋습니다. 너무 어두운 길은 피하는 것이 안전합니다.",
                    true,
                    8,
                    1,
                    true,
                    false,
                    "10분 전",
                    safeKeyword
            );
        }

        if (safeCategory.equals("ALL") || safeCategory.equals("GEAR")) {
            hasItem = appendSearchTipItem(
                    builder,
                    hasItem,
                    3L,
                    103L,
                    "gear_master",
                    false,
                    "NONE",
                    "GEAR",
                    "러닝화는 쿠션보다 발에 맞는지가 먼저입니다",
                    "처음 러닝화를 고를 때는 브랜드보다 발볼, 착화감, 통증 여부를 먼저 확인하는 것이 좋습니다.",
                    false,
                    5,
                    0,
                    false,
                    true,
                    "1시간 전",
                    safeKeyword
            );
        }

        if (safeCategory.equals("ALL") || safeCategory.equals("FREE")) {
            hasItem = appendSearchTipItem(
                    builder,
                    hasItem,
                    4L,
                    104L,
                    "free_runner",
                    false,
                    "NONE",
                    "FREE",
                    "자유 러닝 잡담방",
                    "오늘 러닝하면서 느낀 점을 자유롭게 공유해요.",
                    false,
                    2,
                    1,
                    false,
                    false,
                    "2시간 전",
                    safeKeyword
            );
        }

        builder.append("]");
        return builder.toString();
    }

    /*
     * 팁 검색 결과 JSON 배열에 팁 1개를 조건부로 추가하는 함수임
     */
    private boolean appendSearchTipItem(
            StringBuilder builder,
            boolean hasItem,
            Long tipId,
            Long writerId,
            String nickname,
            boolean badgeOwned,
            String badgeType,
            String category,
            String title,
            String content,
            boolean gpsVisible,
            int likeCount,
            int commentCount,
            boolean liked,
            boolean bookmarked,
            String createdAt,
            String keyword
    ) {
        if (!matchesSearchKeyword(title, content, nickname, keyword)) {
            return hasItem;
        }

        if (hasItem) {
            builder.append(",");
        }

        builder.append("{")
                .append("\"tipId\":").append(tipId).append(",")
                .append("\"writerId\":").append(writerId).append(",")
                .append("\"nickname\":\"").append(escapeJson(nickname)).append("\",")
                .append("\"profileImageUrl\":\"\",")
                .append("\"badgeOwned\":").append(badgeOwned).append(",")
                .append("\"badgeType\":\"").append(escapeJson(badgeType)).append("\",")
                .append("\"category\":\"").append(escapeJson(category)).append("\",")
                .append("\"title\":\"").append(escapeJson(title)).append("\",")
                .append("\"content\":\"").append(escapeJson(content)).append("\",")
                .append("\"gpsVisible\":").append(gpsVisible).append(",")
                .append("\"routeMapImageUrl\":\"\",")
                .append("\"imageUrls\":[],")
                .append("\"likeCount\":").append(likeCount).append(",")
                .append("\"commentCount\":").append(commentCount).append(",")
                .append("\"liked\":").append(liked).append(",")
                .append("\"bookmarked\":").append(bookmarked).append(",")
                .append("\"createdAt\":\"").append(escapeJson(createdAt)).append("\"")
                .append("}");

        return true;
    }

    /*
     * GET /api/community/search/profiles 요청에 대해 반환할 프로필 검색 Mock JSON 데이터임
     */
    private String getMockSearchProfileListJson(String keyword) {
        String safeKeyword = getSafeSearchKeyword(keyword);

        StringBuilder builder = new StringBuilder();
        builder.append("[");

        boolean hasItem = false;

        hasItem = appendSearchUserItem(
                builder,
                hasItem,
                101L,
                "mock_runner",
                "오늘도 네온처럼 달리는 중",
                12,
                "GOLD",
                true,
                safeKeyword
        );

        hasItem = appendSearchUserItem(
                builder,
                hasItem,
                102L,
                "neo_stride",
                "저녁 러닝 좋아합니다.",
                87,
                "SILVER",
                true,
                safeKeyword
        );

        hasItem = appendSearchUserItem(
                builder,
                hasItem,
                103L,
                "gear_master",
                "러닝화랑 장비 리뷰 좋아함",
                42,
                "BRONZE",
                false,
                safeKeyword
        );

        builder.append("]");
        return builder.toString();
    }

    /*
     * GET /api/community/search/friends 요청에 대해 반환할 친구 검색 Mock JSON 데이터임
     */
    private String getMockSearchFriendListJson(String keyword) {
        String safeKeyword = getSafeSearchKeyword(keyword);

        StringBuilder builder = new StringBuilder();
        builder.append("[");

        boolean hasItem = false;

        hasItem = appendSearchUserItem(
                builder,
                hasItem,
                1L,
                "neo_runner",
                "함께 달리는 친구",
                120,
                "GOLD",
                true,
                safeKeyword
        );

        hasItem = appendSearchUserItem(
                builder,
                hasItem,
                2L,
                "marathon_kim",
                "마라톤 준비 중",
                87,
                "SILVER",
                true,
                safeKeyword
        );

        hasItem = appendSearchUserItem(
                builder,
                hasItem,
                3L,
                "night_runner",
                "야간 러닝 선호",
                42,
                "BRONZE",
                true,
                safeKeyword
        );

        builder.append("]");
        return builder.toString();
    }

    /*
     * 프로필/친구 검색 결과 JSON 배열에 유저 1명을 조건부로 추가하는 함수임
     */
    private boolean appendSearchUserItem(
            StringBuilder builder,
            boolean hasItem,
            Long userId,
            String nickname,
            String statusMessage,
            int friendCount,
            String badgeTier,
            boolean friend,
            String keyword
    ) {
        if (!matchesSearchKeyword(nickname, statusMessage, badgeTier, keyword)) {
            return hasItem;
        }

        if (hasItem) {
            builder.append(",");
        }

        builder.append("{")
                .append("\"userId\":").append(userId).append(",")
                .append("\"nickname\":\"").append(escapeJson(nickname)).append("\",")
                .append("\"profileImageUrl\":\"\",")
                .append("\"statusMessage\":\"").append(escapeJson(statusMessage)).append("\",")
                .append("\"friendCount\":").append(friendCount).append(",")
                .append("\"badgeTier\":\"").append(escapeJson(badgeTier)).append("\",")
                .append("\"friend\":").append(friend)
                .append("}");

        return true;
    }

    /*
     * 검색어 null 처리를 담당하는 함수임
     */
    private String getSafeSearchKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }

        return keyword.trim().toLowerCase();
    }

    /*
     * 검색어가 title/content/nickname 중 하나에 포함되는지 확인하는 함수임
     */
    private boolean matchesSearchKeyword(
            String title,
            String content,
            String nickname,
            String keyword
    ) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }

        String safeTitle = title == null ? "" : title.toLowerCase();
        String safeContent = content == null ? "" : content.toLowerCase();
        String safeNickname = nickname == null ? "" : nickname.toLowerCase();

        return safeTitle.contains(keyword)
                || safeContent.contains(keyword)
                || safeNickname.contains(keyword);
    }

    /*
     * JSON 문자열 안에서 문제가 될 수 있는 문자를 이스케이프 처리하는 함수임
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }


    /*
     * 계정관리 Mock JSON 데이터임
     */
    private String getMockAccountJson() {
        return "{"
                + "\"email\":\"mock_runner@neostride.com\","
                + "\"nickname\":\"mock_runner\","
                + "\"profile_photo\":\"\""
                + "}";
    }



}