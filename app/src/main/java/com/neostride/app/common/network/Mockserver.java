package com.neostride.app.common.network;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Mockserver implements Interceptor {

    // 좋아요/북마크 토글 상태 저장 — 클릭마다 진짜로 토글되도록 (앱 재시작 시 초기화)
    private static final Map<String, Boolean> feedLikeState = new HashMap<>();
    private static final Map<String, Boolean> feedBookmarkState = new HashMap<>();
    private static final Map<String, Boolean> tipLikeState = new HashMap<>();
    private static final Map<String, Boolean> tipBookmarkState = new HashMap<>();
    @Override
    public Response intercept(Chain chain) throws IOException {
        String uri = chain.request().url().uri().toString();
        String method = chain.request().method();

        // 1. [러닝 기록] 5/17 코칭(plan_id=1) + 5/16 자유 러닝
        if (uri.contains("records")) {
            String json = "[\n" +
                    // ── 5/17 코칭 러닝 (plan_id=1) — 0.5km / 2:52 (목표 2:30 + 22초 초과) / 평균 5:44/km / 36kcal ──
                    // GPS: 단국대 죽전캠퍼스 정문 부근 (~37.3210, 127.1265) 불규칙 루프
                    "  {\n" +
                    "    \"plan_id\": 1,\n" +
                    "    \"created_at\": \"2026-05-17T09:50:00\", \"total_distance\": 0.50, \"duration\": 172.0, \"pace\": 5.73, \"calories\": 36.0, \"segment_paces\": [5.4, 5.7, 5.9, 5.8],\n" +
                    "    \"gps_traces\": [\n" +
                    "      {\"latitude\": 37.32165, \"longitude\": 127.12635, \"time\": \"2026-05-17T09:50:00\"},\n" +
                    "      {\"latitude\": 37.32152, \"longitude\": 127.12628, \"time\": \"2026-05-17T09:50:14\"},\n" +
                    "      {\"latitude\": 37.32135, \"longitude\": 127.12615, \"time\": \"2026-05-17T09:50:30\"},\n" +
                    "      {\"latitude\": 37.32115, \"longitude\": 127.12602, \"time\": \"2026-05-17T09:50:48\"},\n" +
                    "      {\"latitude\": 37.32098, \"longitude\": 127.12595, \"time\": \"2026-05-17T09:51:05\"},\n" +
                    "      {\"latitude\": 37.32082, \"longitude\": 127.12602, \"time\": \"2026-05-17T09:51:22\"},\n" +
                    "      {\"latitude\": 37.32072, \"longitude\": 127.12620, \"time\": \"2026-05-17T09:51:38\"},\n" +
                    "      {\"latitude\": 37.32078, \"longitude\": 127.12642, \"time\": \"2026-05-17T09:51:55\"},\n" +
                    "      {\"latitude\": 37.32092, \"longitude\": 127.12658, \"time\": \"2026-05-17T09:52:12\"},\n" +
                    "      {\"latitude\": 37.32112, \"longitude\": 127.12665, \"time\": \"2026-05-17T09:52:28\"},\n" +
                    "      {\"latitude\": 37.32135, \"longitude\": 127.12668, \"time\": \"2026-05-17T09:52:42\"},\n" +
                    "      {\"latitude\": 37.32155, \"longitude\": 127.12655, \"time\": \"2026-05-17T09:52:50\"},\n" +
                    "      {\"latitude\": 37.32165, \"longitude\": 127.12635, \"time\": \"2026-05-17T09:52:52\"}\n" +
                    "    ]\n" +
                    "  },\n" +
                    // ── 5/16 자유 러닝 — 0.46km / 2:36 / 5:39/km / 34kcal ──
                    "  {\n" +
                    "    \"plan_id\": null,\n" +
                    "    \"created_at\": \"2026-05-16T10:00:00\", \"total_distance\": 0.46, \"duration\": 156.0, \"pace\": 5.65, \"calories\": 34.0, \"segment_paces\": [5.5, 5.7, 5.6, 5.8],\n" +
                    "    \"gps_traces\": [\n" +
                    "      {\"latitude\": 37.6180, \"longitude\": 126.7120, \"time\": \"2026-05-16T10:00:00\"},\n" +
                    "      {\"latitude\": 37.6183, \"longitude\": 126.7126, \"time\": \"2026-05-16T10:00:20\"},\n" +
                    "      {\"latitude\": 37.6187, \"longitude\": 126.7131, \"time\": \"2026-05-16T10:00:40\"},\n" +
                    "      {\"latitude\": 37.6191, \"longitude\": 126.7135, \"time\": \"2026-05-16T10:01:00\"},\n" +
                    "      {\"latitude\": 37.6195, \"longitude\": 126.7137, \"time\": \"2026-05-16T10:01:20\"},\n" +
                    "      {\"latitude\": 37.6197, \"longitude\": 126.7134, \"time\": \"2026-05-16T10:01:40\"},\n" +
                    "      {\"latitude\": 37.6196, \"longitude\": 126.7128, \"time\": \"2026-05-16T10:02:00\"},\n" +
                    "      {\"latitude\": 37.6192, \"longitude\": 126.7122, \"time\": \"2026-05-16T10:02:20\"},\n" +
                    "      {\"latitude\": 37.6187, \"longitude\": 126.7118, \"time\": \"2026-05-16T10:02:36\"}\n" +
                    "    ]\n" +
                    "  }\n" +
                    "]";
            return buildResponse(chain, json);
        }

        // [목표 상태 변경] PATCH coaching/goals/{goalId}/status
        if (method.equals("PATCH") && uri.matches(".*coaching/goals/\\d+/status.*")) {
            String json = "{\"goal_id\": 123, \"has_active_goal\": false, \"status\": \"completed\"}";
            return buildResponse(chain, json);
        }

        // 2. [코칭 목표] Your Setting: 6주 / 일·화·목·토 / 0.5km / 5:00 페이스
        if (uri.contains("coaching/goals/active")) {
            String json = "{\n" +
                    "  \"goal_id\": 123,\n" +
                    "  \"has_active_goal\": true,\n" +
                    "  \"status\": \"active\",\n" +
                    "  \"goal\": {\n" +
                    "    \"goal_id\": 123, \"period_type\": \"custom\", \"custom_weeks\": 6, \"running_days\": [\"sun\", \"tue\", \"thu\", \"sat\"], \"goal_distance_km\": 0.5, \"goal_pace_min_per_km\": 5.0, \"start_date\": \"2026-05-17\", \"end_date\": \"2026-06-28\"\n" +
                    "  },\n" +
                    "  \"plan_days\": [\n" +
                    // Week 1
                    "    {\"plan_id\": 1,  \"plan_date\": \"2026-05-17\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"첫 코칭을 완료하셨네요! 페이스가 살짝 부족했지만 거리는 완벽히 달성했어요. 해당 기록을 참고하여 다음 러닝 세션인 5월 19일자의 루틴을 완화하여 조정하겠습니다.\", \"is_completed\": true, \"actual_duration_sec\": 172, \"ai_feedback_comment\": \"목표보다 22초 늦었지만 거리 0.5km를 완주했습니다. 다음 세션의 페이스를 완화하여 러닝 루틴을 제공하겠습니다.\"},\n" +
                    "    {\"plan_id\": 2,  \"plan_date\": \"2026-05-19\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.5, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 3,  \"plan_date\": \"2026-05-21\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 4,  \"plan_date\": \"2026-05-23\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    // Week 2
                    "    {\"plan_id\": 5,  \"plan_date\": \"2026-05-24\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 6,  \"plan_date\": \"2026-05-26\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 7,  \"plan_date\": \"2026-05-28\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 8,  \"plan_date\": \"2026-05-30\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    // Week 3
                    "    {\"plan_id\": 9,  \"plan_date\": \"2026-05-31\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 10, \"plan_date\": \"2026-06-02\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 11, \"plan_date\": \"2026-06-04\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 12, \"plan_date\": \"2026-06-06\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    // Week 4
                    "    {\"plan_id\": 13, \"plan_date\": \"2026-06-07\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 14, \"plan_date\": \"2026-06-09\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 15, \"plan_date\": \"2026-06-11\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 16, \"plan_date\": \"2026-06-13\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    // Week 5
                    "    {\"plan_id\": 17, \"plan_date\": \"2026-06-14\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 18, \"plan_date\": \"2026-06-16\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 19, \"plan_date\": \"2026-06-18\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 20, \"plan_date\": \"2026-06-20\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    // Week 6
                    "    {\"plan_id\": 21, \"plan_date\": \"2026-06-21\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 22, \"plan_date\": \"2026-06-23\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 23, \"plan_date\": \"2026-06-25\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 24, \"plan_date\": \"2026-06-27\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false}\n" +
                    "  ]\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // 3. [오늘 플랜] 2026-05-17 (오늘) - plan_id 1번 매핑, 완료 상태
        if (uri.contains("coaching/plans/today")) {
            String json = "{\"has_plan\": true, \"plan_day\": {\"plan_id\": 1, \"plan_date\": \"2026-05-17\", \"day_distance_km\": 0.5, \"day_pace_min_per_km\": 5.0, \"is_completed\": true}, \"goal\": {\"goal_distance_km\": 0.5, \"goal_pace_min_per_km\": 5.0}}";
            return buildResponse(chain, json);
        }

        // [계정 정보 조회] users/me/account
        if (uri.contains("users/me/account")) {
            String json = "{\n" +
                    "  \"email\": \"neostride@example.com\",\n" +
                    "  \"nickname\": \"MocktestID12\",\n" +
                    "  \"profile_photo\": null\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // [닉네임 변경] PATCH users/me/nickname
        if (method.equals("PATCH") && uri.contains("users/me/nickname")) {
            return buildResponse(chain, "{\"status\": \"success\"}");
        }

        // [계정 탈퇴] DELETE users/me
        if (method.equals("DELETE") && uri.contains("users/me")) {
            return buildResponse(chain, "{\"status\": \"success\"}");
        }

        if (method.equals("POST") && uri.contains("community/friends/action")) {
            return buildResponse(chain, "{\"status\": \"success\"}");
        }

        if (method.equals("POST") && uri.contains("feedback")) {
            return buildResponse(chain, "{\"plan_id\": 2, \"is_completed\": true, \"ai_feedback_comment\": \"정확한 세팅 확인 완료!\"}");
        }

        // =====================================================
        // ▼▼▼ 커뮤니티 피드 API (FeedApi 매핑) ▼▼▼
        // =====================================================

        // POST /api/community/feeds/{id}/likes — 피드 좋아요 토글 (실제 상태 토글)
        if (method.equals("POST") && uri.matches(".*community/feeds/\\d+/likes.*")) {
            String feedId = uri.replaceAll(".*community/feeds/(\\d+)/likes.*", "$1");
            boolean current = Boolean.TRUE.equals(feedLikeState.get(feedId));
            boolean newLiked = !current;
            feedLikeState.put(feedId, newLiked);
            int baseCount = 12;
            String json = "{\"feedId\": " + feedId + ", \"liked\": " + newLiked
                    + ", \"likeCount\": " + (baseCount + (newLiked ? 1 : 0)) + "}";
            return buildResponse(chain, json);
        }

        // POST /api/community/feeds/{id}/bookmarks — 피드 북마크 토글 (실제 상태 토글)
        if (method.equals("POST") && uri.matches(".*community/feeds/\\d+/bookmarks.*")) {
            String feedId = uri.replaceAll(".*community/feeds/(\\d+)/bookmarks.*", "$1");
            boolean current = Boolean.TRUE.equals(feedBookmarkState.get(feedId));
            boolean newBookmarked = !current;
            feedBookmarkState.put(feedId, newBookmarked);
            String json = "{\"feedId\": " + feedId + ", \"bookmarked\": " + newBookmarked + "}";
            return buildResponse(chain, json);
        }

        // PUT /api/community/feeds/{id}/comments/{commentId} — 피드 댓글 수정
        if (method.equals("PUT") && uri.matches(".*community/feeds/\\d+/comments/\\d+.*")) {
            String commentId = uri.replaceAll(".*comments/(\\d+).*", "$1");
            String json = "{\n" +
                    "  \"commentId\": " + commentId + ",\n" +
                    "  \"writerId\": 100,\n" +
                    "  \"nickname\": \"MocktestID1557\",\n" +
                    "  \"profileImageUrl\": null,\n" +
                    "  \"badgeOwned\": true,\n" +
                    "  \"badgeType\": \"platinum\",\n" +
                    "  \"content\": \"수정된 댓글 (mock)\",\n" +
                    "  \"createdAt\": \"2026-05-18T11:00:00\",\n" +
                    "  \"mine\": true\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // POST /api/community/feeds/{id}/comments — 피드 댓글 작성
        if (method.equals("POST") && uri.matches(".*community/feeds/\\d+/comments.*")) {
            String json = "{\n" +
                    "  \"commentId\": 9999,\n" +
                    "  \"writerId\": 100,\n" +
                    "  \"nickname\": \"MocktestID1557\",\n" +
                    "  \"profileImageUrl\": null,\n" +
                    "  \"badgeOwned\": true,\n" +
                    "  \"badgeType\": \"platinum\",\n" +
                    "  \"content\": \"방금 작성한 댓글 (mock)\",\n" +
                    "  \"createdAt\": \"2026-05-17T11:00:00\",\n" +
                    "  \"mine\": true\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // GET /api/community/feeds/{id} — 피드 상세 조회
        if (method.equals("GET") && uri.matches(".*community/feeds/\\d+.*")) {
            String feedId = uri.replaceAll(".*community/feeds/(\\d+).*", "$1");
            String json = "{\n" +
                    "  \"feedId\": " + feedId + ",\n" +
                    "  \"writerId\": 41,\n" +
                    "  \"profileImageUrl\": \"https://picsum.photos/100/200\",\n" +
                    "  \"nickname\": \"SpeedKing\",\n" +
                    "  \"badgeOwned\": true,\n" +
                    "  \"badgeType\": \"diamond\",\n" +
                    "  \"createdAt\": \"2026-05-17T18:30:00\",\n" +
                    "  \"title\": \"한강 야경 러닝 5km\",\n" +
                    "  \"content\": \"오늘 한강 야경 보면서 5km 완주했습니다. 페이스 5:20 유지하면서 컨디션 좋았어요. 다음엔 10km 도전!\",\n" +
                    "  \"taggedCount\": 2,\n" +
                    "  \"likeCount\": 12,\n" +
                    "  \"commentCount\": 3,\n" +
                    "  \"liked\": false,\n" +
                    "  \"bookmarked\": false,\n" +
                    "  \"mine\": false,\n" +
                    "  \"distance\": \"5.20km\",\n" +
                    "  \"duration\": \"27:30\",\n" +
                    "  \"pace\": \"5:17/km\",\n" +
                    "  \"mapVisible\": true,\n" +
                    "  \"routeMapImageUri\": \"https://picsum.photos/600/400?random=" + feedId + "\",\n" +
                    "  \"imageUrls\": [\"https://picsum.photos/600/600?random=" + feedId + "1\", \"https://picsum.photos/600/600?random=" + feedId + "2\"],\n" +
                    "  \"comments\": [\n" +
                    "    {\"commentId\": 1, \"writerId\": 42, \"nickname\": \"MorningPace\", \"profileImageUrl\": \"https://picsum.photos/100/201\", \"badgeOwned\": true, \"badgeType\": \"gold\", \"content\": \"멋져요! 다음에 같이 뛰어요\", \"createdAt\": \"2026-05-16T19:00:00\", \"mine\": false},\n" +
                    "    {\"commentId\": 2, \"writerId\": 43, \"nickname\": \"TrailWalker\", \"profileImageUrl\": null, \"badgeOwned\": true, \"badgeType\": \"bronze\", \"content\": \"야경 사진 너무 예쁘네요\", \"createdAt\": \"2026-05-16T19:15:00\", \"mine\": false},\n" +
                    "    {\"commentId\": 3, \"writerId\": 100, \"nickname\": \"MocktestID1557\", \"profileImageUrl\": null, \"badgeOwned\": true, \"badgeType\": \"platinum\", \"content\": \"감사합니다!\", \"createdAt\": \"2026-05-16T19:30:00\", \"mine\": true}\n" +
                    "  ]\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // =====================================================
        // ▼▼▼ 검색 API (SearchApi 매핑) — 반드시 일반 community/feeds, tips 보다 먼저 체크 ▼▼▼
        // =====================================================

        // GET /api/community/search/top-profiles?page=...&size=... — 배지 기준 상위 프로필 (페이지네이션)
        if (method.equals("GET") && uri.contains("community/search/top-profiles")) {
            int tpPage, tpSize;
            try { tpPage = Integer.parseInt(chain.request().url().queryParameter("page")); } catch (Exception e) { tpPage = 0; }
            try { tpSize = Integer.parseInt(chain.request().url().queryParameter("size")); } catch (Exception e) { tpSize = 10; }
            String json = "[\n" +
                    "  {\"user_id\": 51, \"nickname\": \"FlashRunner\",   \"badge_tier\": \"challenger\", \"friend_count\": 120, \"profile_image_url\": \"https://picsum.photos/100/151\", \"status\": \"none\"},\n" +
                    "  {\"user_id\": 52, \"nickname\": \"MasterPace\",    \"badge_tier\": \"master\",     \"friend_count\": 98,  \"profile_image_url\": \"https://picsum.photos/100/152\", \"status\": \"sent\"},\n" +
                    "  {\"user_id\": 41, \"nickname\": \"SpeedKing\",     \"badge_tier\": \"diamond\",    \"friend_count\": 42,  \"profile_image_url\": \"https://picsum.photos/100/141\", \"status\": \"friends\"},\n" +
                    "  {\"user_id\": 53, \"nickname\": \"DiamondLeg\",    \"badge_tier\": \"diamond\",    \"friend_count\": 35,  \"profile_image_url\": \"https://picsum.photos/100/153\", \"status\": \"none\"},\n" +
                    "  {\"user_id\": 45, \"nickname\": \"MaraFan\",       \"badge_tier\": \"platinum\",   \"friend_count\": 56,  \"profile_image_url\": \"https://picsum.photos/100/145\", \"status\": \"none\"},\n" +
                    "  {\"user_id\": 42, \"nickname\": \"MorningPace\",   \"badge_tier\": \"gold\",       \"friend_count\": 18,  \"profile_image_url\": \"https://picsum.photos/100/142\", \"status\": \"friends\"},\n" +
                    "  {\"user_id\": 54, \"nickname\": \"GoldStride\",    \"badge_tier\": \"gold\",       \"friend_count\": 14,  \"profile_image_url\": \"https://picsum.photos/100/154\", \"status\": \"received\"},\n" +
                    "  {\"user_id\": 55, \"nickname\": \"SilverWind\",    \"badge_tier\": \"silver\",     \"friend_count\": 9,   \"profile_image_url\": \"https://picsum.photos/100/155\", \"status\": \"none\"},\n" +
                    "  {\"user_id\": 43, \"nickname\": \"TrailWalker\",   \"badge_tier\": \"silver\",     \"friend_count\": 7,   \"profile_image_url\": null,                            \"status\": \"none\"},\n" +
                    "  {\"user_id\": 44, \"nickname\": \"NightRun77\",    \"badge_tier\": \"bronze\",     \"friend_count\": 3,   \"profile_image_url\": \"https://picsum.photos/100/144\", \"status\": \"friends\"}\n" +
                    "]";
            return buildResponse(chain, paginateItems(json, tpPage, tpSize));
        }

        // GET /api/community/search/my-friends — 내 친구 전체 목록
        if (method.equals("GET") && uri.contains("community/search/my-friends")) {
            String json = "[\n" +
                    "  {\"user_id\": 41, \"nickname\": \"SpeedKing\",   \"badge_tier\": \"diamond\",  \"friend_count\": 42, \"profile_image_url\": \"https://picsum.photos/100/141\", \"status\": \"friends\"},\n" +
                    "  {\"user_id\": 42, \"nickname\": \"MorningPace\", \"badge_tier\": \"gold\",     \"friend_count\": 18, \"profile_image_url\": \"https://picsum.photos/100/142\", \"status\": \"friends\"},\n" +
                    "  {\"user_id\": 44, \"nickname\": \"NightRun77\",  \"badge_tier\": \"bronze\",   \"friend_count\": 3,  \"profile_image_url\": \"https://picsum.photos/100/144\", \"status\": \"friends\"}\n" +
                    "]";
            return buildResponse(chain, json);
        }

        // GET /api/community/search/profiles?keyword=...&page=...&size=... — 프로필 닉네임 검색 (페이지네이션)
        if (method.equals("GET") && uri.contains("community/search/profiles")) {
            String spKeyword = chain.request().url().queryParameter("keyword");
            int spPage, spSize;
            try { spPage = Integer.parseInt(chain.request().url().queryParameter("page")); } catch (Exception e) { spPage = 0; }
            try { spSize = Integer.parseInt(chain.request().url().queryParameter("size")); } catch (Exception e) { spSize = 10; }
            String json = "[" +
                    "{\"user_id\":51,\"nickname\":\"FlashRunner\",\"badge_tier\":\"challenger\",\"friend_count\":120,\"profile_image_url\":\"https://picsum.photos/100/151\",\"status\":\"none\"}," +
                    "{\"user_id\":52,\"nickname\":\"MasterPace\",\"badge_tier\":\"master\",\"friend_count\":98,\"profile_image_url\":\"https://picsum.photos/100/152\",\"status\":\"sent\"}," +
                    "{\"user_id\":41,\"nickname\":\"SpeedKing\",\"badge_tier\":\"diamond\",\"friend_count\":42,\"profile_image_url\":\"https://picsum.photos/100/141\",\"status\":\"friends\"}," +
                    "{\"user_id\":53,\"nickname\":\"DiamondLeg\",\"badge_tier\":\"diamond\",\"friend_count\":35,\"profile_image_url\":\"https://picsum.photos/100/153\",\"status\":\"none\"}," +
                    "{\"user_id\":45,\"nickname\":\"MaraFan\",\"badge_tier\":\"platinum\",\"friend_count\":56,\"profile_image_url\":\"https://picsum.photos/100/145\",\"status\":\"none\"}," +
                    "{\"user_id\":42,\"nickname\":\"MorningPace\",\"badge_tier\":\"gold\",\"friend_count\":18,\"profile_image_url\":\"https://picsum.photos/100/142\",\"status\":\"friends\"}," +
                    "{\"user_id\":54,\"nickname\":\"GoldStride\",\"badge_tier\":\"gold\",\"friend_count\":14,\"profile_image_url\":\"https://picsum.photos/100/154\",\"status\":\"received\"}," +
                    "{\"user_id\":55,\"nickname\":\"SilverWind\",\"badge_tier\":\"silver\",\"friend_count\":9,\"profile_image_url\":\"https://picsum.photos/100/155\",\"status\":\"none\"}," +
                    "{\"user_id\":43,\"nickname\":\"TrailWalker\",\"badge_tier\":\"silver\",\"friend_count\":7,\"profile_image_url\":null,\"status\":\"none\"}," +
                    "{\"user_id\":44,\"nickname\":\"NightRun77\",\"badge_tier\":\"bronze\",\"friend_count\":3,\"profile_image_url\":\"https://picsum.photos/100/144\",\"status\":\"friends\"}" +
                    "]";
            return buildResponse(chain, filterByNickname(json, spKeyword, spPage, spSize));
        }

        // GET /api/community/search/friends?keyword=... — 친구 닉네임 키워드 검색
        if (method.equals("GET") && uri.contains("community/search/friends")) {
            String frKeyword = chain.request().url().queryParameter("keyword");
            String json = "[" +
                    "{\"user_id\":41,\"nickname\":\"SpeedKing\",\"badge_tier\":\"diamond\",\"friend_count\":42,\"profile_image_url\":\"https://picsum.photos/100/141\",\"status\":\"friends\"}," +
                    "{\"user_id\":42,\"nickname\":\"MorningPace\",\"badge_tier\":\"gold\",\"friend_count\":18,\"profile_image_url\":\"https://picsum.photos/100/142\",\"status\":\"friends\"}," +
                    "{\"user_id\":44,\"nickname\":\"NightRun77\",\"badge_tier\":\"bronze\",\"friend_count\":3,\"profile_image_url\":\"https://picsum.photos/100/144\",\"status\":\"friends\"}" +
                    "]";
            // 친구 탭은 페이지네이션 없이 전체 반환 — nickname 기준 필터링만 적용
            return buildResponse(chain, filterByNickname(json, frKeyword, 0, Integer.MAX_VALUE));
        }

        // GET /api/community/search/feeds?keyword=...&page=...&size=... — 피드 검색/최신 목록
        // keyword 없으면 최신순 page+size 슬라이싱, keyword 있으면 title+content 검색 후 page+size 슬라이싱
        if (method.equals("GET") && uri.contains("community/search/feeds")) {
            String keyword  = chain.request().url().queryParameter("keyword");
            int page, size;
            try { page = Integer.parseInt(chain.request().url().queryParameter("page")); } catch (Exception e) { page = 0; }
            try { size = Integer.parseInt(chain.request().url().queryParameter("size")); } catch (Exception e) { size = 10; }

            // 전체 피드 목록 — 최신순, title+content 기준으로 keyword 검색 지원
            String[] allFeeds = {
                "{\"feedId\":1,\"writerId\":41,\"profileImageUrl\":\"https://picsum.photos/100/200\",\"nickname\":\"SpeedKing\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"createdAt\":\"2026-05-17T18:30:00\",\"title\":\"한강 야경 러닝 5km\",\"content\":\"오늘 한강 야경 보면서 5km 완주!\",\"taggedCount\":2,\"likeCount\":12,\"commentCount\":3,\"distance\":\"5.20km\",\"duration\":\"27:30\",\"pace\":\"5:17/km\",\"mapVisible\":true,\"routeMapImageUri\":\"https://picsum.photos/600/400?random=1\",\"imageUrls\":[\"https://picsum.photos/600/600?random=11\"],\"liked\":false,\"bookmarked\":false,\"commented\":true,\"tagged\":false,\"mine\":false}",
                "{\"feedId\":2,\"writerId\":42,\"profileImageUrl\":\"https://picsum.photos/100/210\",\"nickname\":\"MorningPace\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"createdAt\":\"2026-05-17T07:00:00\",\"title\":\"새벽 러닝 7km 완주\",\"content\":\"비 오는 새벽이지만 그래도 뛰었습니다!\",\"taggedCount\":0,\"likeCount\":28,\"commentCount\":7,\"distance\":\"7.00km\",\"duration\":\"38:24\",\"pace\":\"5:29/km\",\"mapVisible\":true,\"routeMapImageUri\":\"https://picsum.photos/600/400?random=2\",\"imageUrls\":[\"https://picsum.photos/600/600?random=21\"],\"liked\":true,\"bookmarked\":false,\"commented\":false,\"tagged\":false,\"mine\":false}",
                "{\"feedId\":3,\"writerId\":44,\"profileImageUrl\":\"https://picsum.photos/100/220\",\"nickname\":\"NightRun77\",\"badgeOwned\":false,\"badgeType\":null,\"createdAt\":\"2026-05-16T22:10:00\",\"title\":\"야간 러닝 인터벌 훈련\",\"content\":\"400m × 8 인터벌 마쳤어요. 다리 후들...\",\"taggedCount\":1,\"likeCount\":9,\"commentCount\":2,\"distance\":\"4.80km\",\"duration\":\"22:15\",\"pace\":\"4:38/km\",\"mapVisible\":false,\"routeMapImageUri\":null,\"imageUrls\":[],\"liked\":false,\"bookmarked\":true,\"commented\":false,\"tagged\":true,\"mine\":false}",
                "{\"feedId\":4,\"writerId\":100,\"profileImageUrl\":null,\"nickname\":\"MocktestID1557\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"createdAt\":\"2026-05-16T11:00:00\",\"title\":\"오늘의 코스 추천\",\"content\":\"단국대 근처 죽전로 코스 진짜 좋아요\",\"taggedCount\":3,\"likeCount\":5,\"commentCount\":1,\"distance\":\"3.20km\",\"duration\":\"18:00\",\"pace\":\"5:37/km\",\"mapVisible\":true,\"routeMapImageUri\":\"https://picsum.photos/600/400?random=4\",\"imageUrls\":[\"https://picsum.photos/600/600?random=41\"],\"liked\":true,\"bookmarked\":true,\"commented\":true,\"tagged\":false,\"mine\":true}",
                "{\"feedId\":5,\"writerId\":51,\"profileImageUrl\":\"https://picsum.photos/100/230\",\"nickname\":\"FlashRunner\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"createdAt\":\"2026-05-15T20:00:00\",\"title\":\"마라톤 준비 20km 롱런\",\"content\":\"첫 마라톤 준비 중 20km 장거리 완주했어요! 35km 이후가 두렵지만 해볼게요.\",\"taggedCount\":1,\"likeCount\":41,\"commentCount\":9,\"distance\":\"20.10km\",\"duration\":\"1:51:00\",\"pace\":\"5:32/km\",\"mapVisible\":true,\"routeMapImageUri\":\"https://picsum.photos/600/400?random=5\",\"imageUrls\":[\"https://picsum.photos/600/600?random=51\"],\"liked\":false,\"bookmarked\":true,\"commented\":false,\"tagged\":false,\"mine\":false}",
                "{\"feedId\":6,\"writerId\":52,\"profileImageUrl\":\"https://picsum.photos/100/240\",\"nickname\":\"MasterPace\",\"badgeOwned\":true,\"badgeType\":\"master\",\"createdAt\":\"2026-05-15T17:30:00\",\"title\":\"올림픽공원 4.7km 코스\",\"content\":\"올림픽공원 순환 코스 최고! 나무 그늘 덕에 한여름에도 시원해요.\",\"taggedCount\":0,\"likeCount\":34,\"commentCount\":5,\"distance\":\"4.70km\",\"duration\":\"25:08\",\"pace\":\"5:21/km\",\"mapVisible\":true,\"routeMapImageUri\":\"https://picsum.photos/600/400?random=6\",\"imageUrls\":[],\"liked\":false,\"bookmarked\":false,\"commented\":false,\"tagged\":false,\"mine\":false}",
                "{\"feedId\":7,\"writerId\":45,\"profileImageUrl\":\"https://picsum.photos/100/250\",\"nickname\":\"MaraFan\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"createdAt\":\"2026-05-15T08:00:00\",\"title\":\"한강 반포 10km 인증\",\"content\":\"반포 한강공원에서 10km 달렸어요. 날씨 완벽하고 야경도 예뻤어요.\",\"taggedCount\":2,\"likeCount\":57,\"commentCount\":11,\"distance\":\"10.00km\",\"duration\":\"54:20\",\"pace\":\"5:26/km\",\"mapVisible\":true,\"routeMapImageUri\":\"https://picsum.photos/600/400?random=7\",\"imageUrls\":[\"https://picsum.photos/600/600?random=71\"],\"liked\":true,\"bookmarked\":false,\"commented\":false,\"tagged\":false,\"mine\":false}",
                "{\"feedId\":8,\"writerId\":43,\"profileImageUrl\":\"https://picsum.photos/100/260\",\"nickname\":\"TrailWalker\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"createdAt\":\"2026-05-14T19:00:00\",\"title\":\"남산 오르막 러닝 4.5km\",\"content\":\"남산 순환 코스 경사가 장난 아니에요. 허벅지가 터질 것 같았지만 야경에 힐링했어요.\",\"taggedCount\":0,\"likeCount\":22,\"commentCount\":4,\"distance\":\"4.50km\",\"duration\":\"28:45\",\"pace\":\"6:23/km\",\"mapVisible\":true,\"routeMapImageUri\":\"https://picsum.photos/600/400?random=8\",\"imageUrls\":[\"https://picsum.photos/600/600?random=81\"],\"liked\":false,\"bookmarked\":false,\"commented\":false,\"tagged\":false,\"mine\":false}",
                "{\"feedId\":9,\"writerId\":41,\"profileImageUrl\":\"https://picsum.photos/100/200\",\"nickname\":\"SpeedKing\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"createdAt\":\"2026-05-14T06:30:00\",\"title\":\"5km PB 경신! 22:30\",\"content\":\"인터벌 훈련 3개월 만에 5km PB를 22분 30초로 단축했어요!\",\"taggedCount\":4,\"likeCount\":93,\"commentCount\":18,\"distance\":\"5.00km\",\"duration\":\"22:30\",\"pace\":\"4:30/km\",\"mapVisible\":false,\"routeMapImageUri\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=91\",\"https://picsum.photos/600/600?random=92\"],\"liked\":false,\"bookmarked\":false,\"commented\":false,\"tagged\":false,\"mine\":false}",
                "{\"feedId\":10,\"writerId\":42,\"profileImageUrl\":\"https://picsum.photos/100/210\",\"nickname\":\"MorningPace\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"createdAt\":\"2026-05-13T21:30:00\",\"title\":\"비 오는 날 야간 러닝\",\"content\":\"폭우 속 야간 러닝 강행. 젖은 신발이 무거웠지만 시원했어요.\",\"taggedCount\":0,\"likeCount\":31,\"commentCount\":6,\"distance\":\"5.20km\",\"duration\":\"29:10\",\"pace\":\"5:36/km\",\"mapVisible\":false,\"routeMapImageUri\":null,\"imageUrls\":[],\"liked\":false,\"bookmarked\":true,\"commented\":false,\"tagged\":false,\"mine\":false}",
                "{\"feedId\":11,\"writerId\":44,\"profileImageUrl\":\"https://picsum.photos/100/220\",\"nickname\":\"NightRun77\",\"badgeOwned\":false,\"badgeType\":null,\"createdAt\":\"2026-05-13T20:00:00\",\"title\":\"잠실 한강 야경 6km\",\"content\":\"잠실 한강공원 6km 달렸어요. 야경이 너무 예쁘고 바람도 시원했어요.\",\"taggedCount\":1,\"likeCount\":45,\"commentCount\":8,\"distance\":\"6.00km\",\"duration\":\"33:00\",\"pace\":\"5:30/km\",\"mapVisible\":true,\"routeMapImageUri\":\"https://picsum.photos/600/400?random=11\",\"imageUrls\":[],\"liked\":true,\"bookmarked\":false,\"commented\":true,\"tagged\":false,\"mine\":false}",
                "{\"feedId\":12,\"writerId\":51,\"profileImageUrl\":\"https://picsum.photos/100/230\",\"nickname\":\"FlashRunner\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"createdAt\":\"2026-05-12T05:30:00\",\"title\":\"새벽 5시 기상 5km\",\"content\":\"새벽 5시에 기상해서 5km 달리고 출근! 하루가 두 배 긴 느낌이에요.\",\"taggedCount\":0,\"likeCount\":38,\"commentCount\":7,\"distance\":\"5.00km\",\"duration\":\"26:45\",\"pace\":\"5:21/km\",\"mapVisible\":false,\"routeMapImageUri\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=121\"],\"liked\":false,\"bookmarked\":false,\"commented\":false,\"tagged\":false,\"mine\":false}",
                "{\"feedId\":13,\"writerId\":100,\"profileImageUrl\":null,\"nickname\":\"MocktestID1557\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"createdAt\":\"2026-05-12T19:00:00\",\"title\":\"집 근처 공원 조깅 3km\",\"content\":\"퇴근 후 집 근처 공원 3km 조깅. 가볍게 달렸어요. 공원 조명이 켜지니까 야경 분위기.\",\"taggedCount\":0,\"likeCount\":7,\"commentCount\":1,\"distance\":\"3.00km\",\"duration\":\"17:30\",\"pace\":\"5:50/km\",\"mapVisible\":false,\"routeMapImageUri\":null,\"imageUrls\":[],\"liked\":false,\"bookmarked\":false,\"commented\":false,\"tagged\":false,\"mine\":true}",
                "{\"feedId\":14,\"writerId\":52,\"profileImageUrl\":\"https://picsum.photos/100/240\",\"nickname\":\"MasterPace\",\"badgeOwned\":true,\"badgeType\":\"master\",\"createdAt\":\"2026-05-11T07:00:00\",\"title\":\"인터벌 400m×8 완료\",\"content\":\"400m 인터벌 8세트 완료. 마지막 두 세트가 제일 힘들었어요. 다음 주엔 10세트 도전!\",\"taggedCount\":2,\"likeCount\":52,\"commentCount\":10,\"distance\":\"5.40km\",\"duration\":\"24:00\",\"pace\":\"4:27/km\",\"mapVisible\":false,\"routeMapImageUri\":null,\"imageUrls\":[],\"liked\":false,\"bookmarked\":false,\"commented\":false,\"tagged\":true,\"mine\":false}",
                "{\"feedId\":15,\"writerId\":45,\"profileImageUrl\":\"https://picsum.photos/100/250\",\"nickname\":\"MaraFan\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"createdAt\":\"2026-05-10T08:30:00\",\"title\":\"광교 호수공원 8km\",\"content\":\"광교 호수공원에서 8km! 두 호수를 한 바퀴 도는 코스인데 호수 뷰가 너무 좋아요.\",\"taggedCount\":1,\"likeCount\":63,\"commentCount\":12,\"distance\":\"8.00km\",\"duration\":\"44:00\",\"pace\":\"5:30/km\",\"mapVisible\":true,\"routeMapImageUri\":\"https://picsum.photos/600/400?random=15\",\"imageUrls\":[\"https://picsum.photos/600/600?random=151\"],\"liked\":false,\"bookmarked\":true,\"commented\":false,\"tagged\":false,\"mine\":false}"
            };

            // keyword 필터링: title + content 필드 값을 기준으로 검색 (대소문자·한글 무관)
            boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
            String lowerKeyword = hasKeyword ? keyword.trim().toLowerCase() : "";

            java.util.List<String> filtered = new java.util.ArrayList<>();
            for (String feed : allFeeds) {
                if (!hasKeyword) {
                    filtered.add(feed);
                } else {
                    // "title":"..." 과 "content":"..." 값만 추출해서 검색
                    String titleVal = "";
                    String contentVal = "";
                    java.util.regex.Matcher tm = java.util.regex.Pattern.compile("\"title\":\"([^\"]*)\"").matcher(feed);
                    if (tm.find()) titleVal = tm.group(1).toLowerCase();
                    java.util.regex.Matcher cm = java.util.regex.Pattern.compile("\"content\":\"([^\"]*)\"").matcher(feed);
                    if (cm.find()) contentVal = cm.group(1).toLowerCase();
                    if (titleVal.contains(lowerKeyword) || contentVal.contains(lowerKeyword)) {
                        filtered.add(feed);
                    }
                }
            }

            // page+size 슬라이싱 (keyword 유무와 관계없이 동일하게 적용)
            int from = page * size;
            int to   = Math.min(from + size, filtered.size());
            if (from >= filtered.size()) return buildResponse(chain, "[]");
            filtered = filtered.subList(from, to);

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < filtered.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(filtered.get(i));
            }
            sb.append("]");
            return buildResponse(chain, sb.toString());
        }

        // GET /api/community/search/tips?keyword=...&category=...&page=...&size=... — 팁 검색/최신 목록
        if (method.equals("GET") && uri.contains("community/search/tips")) {
            String categoryParam = chain.request().url().queryParameter("category");
            if (categoryParam != null) categoryParam = categoryParam.toUpperCase();
            else categoryParam = "";
            String tipKeyword = chain.request().url().queryParameter("keyword");
            int tipPage, tipSize;
            try { tipPage = Integer.parseInt(chain.request().url().queryParameter("page")); } catch (Exception e) { tipPage = 0; }
            try { tipSize = Integer.parseInt(chain.request().url().queryParameter("size")); } catch (Exception e) { tipSize = 10; }
            String json;
            if (categoryParam.equals("TRAINING")) {
                // 훈련 10개
                json = "[\n" +
                        "  {\"tipId\":101,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"훈련\",\"title\":\"러닝 호흡법: 2:2 패턴이 최고\",\"content\":\"들숨 2보 날숨 2보 패턴으로 리듬을 잡으면 페이스 유지가 훨씬 쉬워집니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=tr1\"],\"likeCount\":88,\"commentCount\":14,\"liked\":false,\"bookmarked\":true,\"commented\":true,\"mine\":false,\"createdAt\":\"2026-05-17T10:00:00\"},\n" +
                        "  {\"tipId\":102,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"훈련\",\"title\":\"인터벌 트레이닝 입문 가이드\",\"content\":\"400m 전력질주 후 200m 조깅, 8세트 반복이 기본입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=tr2\"],\"likeCount\":72,\"commentCount\":9,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-16T09:00:00\"},\n" +
                        "  {\"tipId\":103,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"훈련\",\"title\":\"템포 런으로 젖산 역치 올리기\",\"content\":\"편안한 페이스보다 15~20초 빠른 속도로 20분 달리는 게 핵심입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":61,\"commentCount\":7,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-15T08:00:00\"},\n" +
                        "  {\"tipId\":104,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"훈련\",\"title\":\"빌드업 런: 점진적 페이스업 훈련\",\"content\":\"처음 2km는 여유롭게, 마지막 2km는 최대 속도로 마무리하는 훈련입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=tr4\"],\"likeCount\":55,\"commentCount\":6,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-14T07:30:00\"},\n" +
                        "  {\"tipId\":105,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"훈련\",\"title\":\"5km 최단기간 완주하는 법\",\"content\":\"걷기 1분 + 달리기 2분을 반복하다가 점점 달리는 시간을 늘려가세요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=tr5\"],\"likeCount\":49,\"commentCount\":11,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-13T06:00:00\"},\n" +
                        "  {\"tipId\":106,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"훈련\",\"title\":\"다리 근력 강화 루틴 3가지\",\"content\":\"스쿼트·런지·카프레이즈를 주 3회 꾸준히 하면 러닝 속도가 올라갑니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":43,\"commentCount\":5,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-12T20:00:00\"},\n" +
                        "  {\"tipId\":107,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"훈련\",\"title\":\"러닝 후 젖산 빠르게 제거하는 법\",\"content\":\"완전 정지보다 가벼운 조깅으로 쿨다운하면 젖산 제거 속도가 2배 빠릅니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=tr7\"],\"likeCount\":37,\"commentCount\":4,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-11T19:00:00\"},\n" +
                        "  {\"tipId\":108,\"writerId\":44,\"nickname\":\"NightRun77\",\"profileImageUrl\":\"https://picsum.photos/100/318\",\"badgeOwned\":false,\"badgeType\":null,\"category\":\"훈련\",\"title\":\"심박수 기반 러닝 훈련법\",\"content\":\"Zone 2(최대 심박수 60~70%) 훈련이 지구력 향상에 가장 효과적입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":31,\"commentCount\":3,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-10T21:00:00\"},\n" +
                        "  {\"tipId\":109,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"훈련\",\"title\":\"겨울 러닝 훈련 주의사항\",\"content\":\"기온 -5도 이하에선 페이스를 10~15% 낮추고 레이어링에 신경 쓰세요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=tr9\"],\"likeCount\":26,\"commentCount\":2,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-09T08:00:00\"},\n" +
                        "  {\"tipId\":110,\"writerId\":100,\"nickname\":\"MocktestID1557\",\"profileImageUrl\":\"https://picsum.photos/100/100\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"훈련\",\"title\":\"내가 직접 쓴 훈련 팁\",\"content\":\"매일 1km씩 늘려가는 방법으로 한 달 만에 10km를 달렸습니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":5,\"commentCount\":1,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":true,\"createdAt\":\"2026-05-08T12:00:00\"}\n" +
                        "]";
            } else if (categoryParam.equals("COURSE")) {
                // 코스 10개
                json = "[\n" +
                        "  {\"tipId\":201,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"코스\",\"title\":\"단국대 죽전로 야경 코스 추천\",\"content\":\"단국대 정문에서 출발해서 죽전로 한 바퀴 도는 5km 코스입니다.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=co1\",\"imageUrls\":[\"https://picsum.photos/600/600?random=co1a\"],\"likeCount\":95,\"commentCount\":18,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-17T19:00:00\"},\n" +
                        "  {\"tipId\":202,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"코스\",\"title\":\"한강 반포 야경 코스 5km\",\"content\":\"반포 한강공원 주차장 → 잠수교 → 반환점 → 원점 복귀, 평탄해서 초보도 OK.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=co2\",\"imageUrls\":[\"https://picsum.photos/600/600?random=co2a\"],\"likeCount\":82,\"commentCount\":15,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-16T18:30:00\"},\n" +
                        "  {\"tipId\":203,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"코스\",\"title\":\"올림픽공원 순환 코스 4.7km\",\"content\":\"올림픽공원 동문 → 평화의 광장 → 몽촌토성길 → 동문, 나무가 많아 여름에도 좋아요.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=co3\",\"imageUrls\":[],\"likeCount\":74,\"commentCount\":12,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-15T17:00:00\"},\n" +
                        "  {\"tipId\":204,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"코스\",\"title\":\"서울숲 산책 러닝 코스 3km\",\"content\":\"서울숲 1문 → 꽃사슴 방사장 → 수변 광장 → 1문 복귀, 3km 짧은 코스.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=co4\",\"imageUrls\":[\"https://picsum.photos/600/600?random=co4a\"],\"likeCount\":63,\"commentCount\":9,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-14T16:00:00\"},\n" +
                        "  {\"tipId\":205,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"코스\",\"title\":\"남산 순환 코스 4.5km\",\"content\":\"남산도서관 → N서울타워 → 남산공원 북측 순환로, 경사 있어 칼로리 소모 많아요.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=co5\",\"imageUrls\":[\"https://picsum.photos/600/600?random=co5a\"],\"likeCount\":57,\"commentCount\":8,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-13T15:00:00\"},\n" +
                        "  {\"tipId\":206,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"코스\",\"title\":\"잠실 한강공원 야간 코스 6km\",\"content\":\"잠실대교 남단 → 잠실 한강공원 → 천호대교 → 복귀, 야경 최고!\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=co6\",\"imageUrls\":[],\"likeCount\":51,\"commentCount\":7,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-12T20:00:00\"},\n" +
                        "  {\"tipId\":207,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"코스\",\"title\":\"광교 호수공원 순환 코스 8km\",\"content\":\"광교 호수공원 메인 광장에서 출발해 두 호수를 한 바퀴 도는 8km 루트.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=co7\",\"imageUrls\":[\"https://picsum.photos/600/600?random=co7a\"],\"likeCount\":44,\"commentCount\":6,\"liked\":true,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-11T07:00:00\"},\n" +
                        "  {\"tipId\":208,\"writerId\":44,\"nickname\":\"NightRun77\",\"profileImageUrl\":\"https://picsum.photos/100/318\",\"badgeOwned\":false,\"badgeType\":null,\"category\":\"코스\",\"title\":\"분당 탄천 코스 10km\",\"content\":\"야탑역 → 탄천 자전거도로 → 성남종합운동장 → 복귀, 평탄한 10km.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=co8\",\"imageUrls\":[],\"likeCount\":38,\"commentCount\":4,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-10T06:30:00\"},\n" +
                        "  {\"tipId\":209,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"코스\",\"title\":\"인천 송도 해변 코스 7km\",\"content\":\"센트럴파크 → 해변공원 → 바다 방향 산책로 → 복귀, 바다 바람이 일품.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=co9\",\"imageUrls\":[\"https://picsum.photos/600/600?random=co9a\"],\"likeCount\":29,\"commentCount\":3,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-09T17:00:00\"},\n" +
                        "  {\"tipId\":210,\"writerId\":100,\"nickname\":\"MocktestID1557\",\"profileImageUrl\":\"https://picsum.photos/100/100\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"코스\",\"title\":\"내가 직접 쓴 코스 추천\",\"content\":\"집 근처 공원 2바퀴가 딱 5km! 매일 아침 이 코스 돕니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":4,\"commentCount\":0,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":true,\"createdAt\":\"2026-05-08T07:00:00\"}\n" +
                        "]";
            } else if (categoryParam.equals("GEAR")) {
                // 장비 10개
                json = "[\n" +
                        "  {\"tipId\":301,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"장비\",\"title\":\"초보 러너 러닝화 추천 Top 3\",\"content\":\"아식스 젤 카야노, 브룩스 고스트, 뉴발란스 1080이 쿠셔닝 좋아 입문자에게 최적.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=ge1\"],\"likeCount\":102,\"commentCount\":22,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-17T11:00:00\"},\n" +
                        "  {\"tipId\":302,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"장비\",\"title\":\"GPS 워치 비교: 가민 vs 애플워치\",\"content\":\"가민은 배터리, 애플은 생태계. 순수 러닝 목적이면 가민 포어러너 추천.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=ge2\"],\"likeCount\":89,\"commentCount\":17,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-16T10:00:00\"},\n" +
                        "  {\"tipId\":303,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"장비\",\"title\":\"겨울 러닝 레이어링 완벽 가이드\",\"content\":\"베이스레이어(기모) + 미드레이어(플리스) + 방풍 아우터 3단 레이어가 정석.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":76,\"commentCount\":13,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-15T09:00:00\"},\n" +
                        "  {\"tipId\":304,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"장비\",\"title\":\"러닝 양말 왜 중요한가\",\"content\":\"발의 블리스터를 막으려면 면 양말 대신 기능성 메리노울 양말을 써야 해요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=ge4\"],\"likeCount\":64,\"commentCount\":10,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-14T08:00:00\"},\n" +
                        "  {\"tipId\":305,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"장비\",\"title\":\"압박 레깅스 효과 있나요?\",\"content\":\"종아리 압박 슬리브나 압박 타이츠는 근피로 완화와 부종 방지에 실제로 효과 있습니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":55,\"commentCount\":8,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-13T07:00:00\"},\n" +
                        "  {\"tipId\":306,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"장비\",\"title\":\"에너지 젤 언제 먹어야 하나\",\"content\":\"장거리(10km 이상)에서 40~45분마다 1개씩, 물과 함께 섭취하세요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=ge6\"],\"likeCount\":47,\"commentCount\":7,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-12T06:00:00\"},\n" +
                        "  {\"tipId\":307,\"writerId\":44,\"nickname\":\"NightRun77\",\"profileImageUrl\":\"https://picsum.photos/100/318\",\"badgeOwned\":false,\"badgeType\":null,\"category\":\"장비\",\"title\":\"야간 러닝 반사 조끼 필수템\",\"content\":\"시인성 확보를 위해 360도 반사 조끼 필수. 저렴한 건 5천 원이면 구매 가능.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":39,\"commentCount\":5,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-11T21:00:00\"},\n" +
                        "  {\"tipId\":308,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"장비\",\"title\":\"러닝 벨트 & 파우치 추천\",\"content\":\"네이선 스피드샷 파우치 또는 살로몬 런닝 조끼가 흔들림 없어 가장 인기.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=ge8\"],\"likeCount\":33,\"commentCount\":4,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-10T10:00:00\"},\n" +
                        "  {\"tipId\":309,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"장비\",\"title\":\"러닝 이어폰: 골전도 vs 인이어\",\"content\":\"야외 러닝엔 골전도(샥즈 오픈런), 트레드밀엔 인이어가 안정적입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":27,\"commentCount\":3,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-09T09:00:00\"},\n" +
                        "  {\"tipId\":310,\"writerId\":100,\"nickname\":\"MocktestID1557\",\"profileImageUrl\":\"https://picsum.photos/100/100\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"장비\",\"title\":\"내가 직접 작성한 장비 팁\",\"content\":\"초보자가 5:00 페이스 도전할 때 카본 플레이트 슈즈는 오히려 부상 위험.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":3,\"commentCount\":0,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":true,\"createdAt\":\"2026-05-08T15:00:00\"}\n" +
                        "]";
            } else if (categoryParam.equals("FREE")) {
                // 자유 30개
                json = "[\n" +
                        "  {\"tipId\":401,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"자유\",\"title\":\"러닝 전후 필수 스트레칭 5가지\",\"content\":\"종아리·햄스트링·고관절·발목·어깨 순서로 10초씩 풀어주세요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=fr1\"],\"likeCount\":120,\"commentCount\":24,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-17T20:00:00\"},\n" +
                        "  {\"tipId\":402,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"자유\",\"title\":\"마라톤 첫 완주 후기\",\"content\":\"42.195km 완주! 35km부터 다리가 안 풀렸지만 포기하지 않았습니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=fr2\"],\"likeCount\":108,\"commentCount\":21,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-17T09:00:00\"},\n" +
                        "  {\"tipId\":403,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"자유\",\"title\":\"비 오는 날 러닝 꿀팁\",\"content\":\"방수 모자 + 경량 윈드브레이커면 충분. 발은 어차피 젖으니 메시 소재 신발이 오히려 편해요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":95,\"commentCount\":18,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-16T18:00:00\"},\n" +
                        "  {\"tipId\":404,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"자유\",\"title\":\"러닝 뮤직 플레이리스트 공유\",\"content\":\"BPM 160~175 곡들로 구성하면 케이던스 유지에 딱 맞아요. 스포티파이 러닝 믹스 추천.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":88,\"commentCount\":16,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-16T07:00:00\"},\n" +
                        "  {\"tipId\":405,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"자유\",\"title\":\"러닝 다이어트, 진짜 효과 있나요?\",\"content\":\"주 3회 5km 6개월 기준 체지방 8% 감소. 식단 병행이 핵심입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=fr5\"],\"likeCount\":81,\"commentCount\":15,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-15T21:00:00\"},\n" +
                        "  {\"tipId\":406,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"자유\",\"title\":\"처음으로 10km 완주했어요!\",\"content\":\"3개월 전에 1km도 못 뛰었는데 오늘 10km 완주! 꾸준함이 답이에요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":74,\"commentCount\":13,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-15T17:00:00\"},\n" +
                        "  {\"tipId\":407,\"writerId\":44,\"nickname\":\"NightRun77\",\"profileImageUrl\":\"https://picsum.photos/100/318\",\"badgeOwned\":false,\"badgeType\":null,\"category\":\"자유\",\"title\":\"무릎이 아플 때 러닝 어떻게 하나요?\",\"content\":\"무릎 통증 시엔 무조건 쉬어야 해요. 2주 휴식 후 천천히 재개가 원칙.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":67,\"commentCount\":12,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-15T10:00:00\"},\n" +
                        "  {\"tipId\":408,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"자유\",\"title\":\"러닝 전 뭐 먹어야 할까요\",\"content\":\"2시간 전 가벼운 탄수화물(바나나, 식빵)이 최적. 공복 러닝은 지방 연소엔 좋지만 강도 높이기 어려워요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":62,\"commentCount\":11,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-14T08:00:00\"},\n" +
                        "  {\"tipId\":409,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"자유\",\"title\":\"오늘 PB 경신했습니다! 5km 22분\",\"content\":\"6개월 만에 5km PB를 22분으로 줄였어요. 인터벌이 핵심이었습니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=fr9\"],\"likeCount\":58,\"commentCount\":10,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-14T19:00:00\"},\n" +
                        "  {\"tipId\":410,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"자유\",\"title\":\"야간 러닝 안전하게 즐기는 법\",\"content\":\"반사 조끼 착용, 밝은 색 의류, 이어폰 한쪽만, 인도 내 달리기가 기본 수칙.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":54,\"commentCount\":9,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-14T07:00:00\"},\n" +
                        "  {\"tipId\":411,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"자유\",\"title\":\"여름 러닝 더위 극복 방법\",\"content\":\"새벽 5~7시 또는 밤 9시 이후로 시간 조정, 수분 보충은 20분마다 한 모금씩.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":51,\"commentCount\":8,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-13T06:00:00\"},\n" +
                        "  {\"tipId\":412,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"자유\",\"title\":\"러닝 슬럼프 어떻게 극복했나요\",\"content\":\"목표를 '완주'에서 '즐기기'로 바꾸니 슬럼프가 사라졌어요. 결과보다 과정.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":48,\"commentCount\":7,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-13T20:00:00\"},\n" +
                        "  {\"tipId\":413,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"자유\",\"title\":\"첫 하프마라톤 준비 3개월 플랜\",\"content\":\"1주차: 주 3회 5km, 6주차: 주 4회 10km, 10주차: 하프 거리 18km 롱런이 표준 플랜.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=fr13\"],\"likeCount\":45,\"commentCount\":6,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-13T11:00:00\"},\n" +
                        "  {\"tipId\":414,\"writerId\":44,\"nickname\":\"NightRun77\",\"profileImageUrl\":\"https://picsum.photos/100/318\",\"badgeOwned\":false,\"badgeType\":null,\"category\":\"자유\",\"title\":\"러닝 후 근육통 관리법\",\"content\":\"아이스배스 10분 또는 폼롤러 마사지 15분이 가장 효과적입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":42,\"commentCount\":5,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-12T22:00:00\"},\n" +
                        "  {\"tipId\":415,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"자유\",\"title\":\"러닝 크루 서울 강남 모집\",\"content\":\"매주 토요일 오전 7시 코엑스 집결. 10km 내외 페이스 런, 초보 환영!\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":39,\"commentCount\":4,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-12T14:00:00\"},\n" +
                        "  {\"tipId\":416,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"자유\",\"title\":\"러닝 기록 앱 비교: 나이키 vs 스트라바\",\"content\":\"나이키런 클럽은 초보 코칭 강점, 스트라바는 소셜 기능과 세그먼트 분석이 강점.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":36,\"commentCount\":4,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-12T07:00:00\"},\n" +
                        "  {\"tipId\":417,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"자유\",\"title\":\"달리기 자세 체크 포인트 5가지\",\"content\":\"시선 전방 15m, 어깨 이완, 팔꿈치 90도, 발 착지 무릎 아래, 코어 수축이 핵심.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=fr17\"],\"likeCount\":33,\"commentCount\":3,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-11T20:00:00\"},\n" +
                        "  {\"tipId\":418,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"자유\",\"title\":\"스트라이드 vs 케이던스 논쟁\",\"content\":\"케이던스 180spm 고정보다 자신에게 맞는 리듬 찾는 게 더 중요합니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":30,\"commentCount\":3,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-11T08:00:00\"},\n" +
                        "  {\"tipId\":419,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"자유\",\"title\":\"크로스트레이닝 병행 러너 있나요\",\"content\":\"수영 주 1회 병행하니 호흡 능력이 확실히 좋아졌어요. 관절에도 부담 적고요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":27,\"commentCount\":2,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-10T19:00:00\"},\n" +
                        "  {\"tipId\":420,\"writerId\":44,\"nickname\":\"NightRun77\",\"profileImageUrl\":\"https://picsum.photos/100/318\",\"badgeOwned\":false,\"badgeType\":null,\"category\":\"자유\",\"title\":\"레이스 전날 준비 루틴\",\"content\":\"전날 저녁 탄수화물 부하, 충분한 수면, 새 장비는 절대 사용 금지.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":24,\"commentCount\":2,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-10T07:00:00\"},\n" +
                        "  {\"tipId\":421,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"자유\",\"title\":\"달리기 시작하는 분들께 드리는 글\",\"content\":\"처음엔 느려도 괜찮아요. 완주 자체가 목표입니다. 1년 후의 나를 믿으세요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":21,\"commentCount\":2,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-09T21:00:00\"},\n" +
                        "  {\"tipId\":422,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"자유\",\"title\":\"러닝 호흡이 너무 힘들어요\",\"content\":\"코로 들이마시고 입으로 내쉬는 것보다 입으로 들이마시는 게 산소 공급에 더 효율적.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":19,\"commentCount\":2,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-09T10:00:00\"},\n" +
                        "  {\"tipId\":423,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"자유\",\"title\":\"날씨별 러닝 팁 총정리\",\"content\":\"맑은 날: 선글라스·선크림, 흐린 날: 그냥 뛰어도 OK, 비: 경량 방수, 눈: 미끄럼 주의.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":17,\"commentCount\":1,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-08T20:00:00\"},\n" +
                        "  {\"tipId\":424,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"자유\",\"title\":\"체력이 너무 빨리 떨어져요\",\"content\":\"VO2max가 낮으면 금방 지칩니다. Zone 2 러닝으로 유산소 기저 능력을 먼저 키우세요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":15,\"commentCount\":1,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-08T09:00:00\"},\n" +
                        "  {\"tipId\":425,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"자유\",\"title\":\"러닝 커뮤니티 추천해드려요\",\"content\":\"스트라바 클럽, 러닝 서울, 나이키런클럽 등 다양한 온오프 커뮤니티 있어요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":13,\"commentCount\":1,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-07T21:00:00\"},\n" +
                        "  {\"tipId\":426,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"자유\",\"title\":\"새벽 러닝 vs 저녁 러닝 뭐가 나을까\",\"content\":\"새벽: 공복 지방연소 유리, 저녁: 체온·근력 최고조라 기록 향상에 유리.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":11,\"commentCount\":1,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-07T07:00:00\"},\n" +
                        "  {\"tipId\":427,\"writerId\":44,\"nickname\":\"NightRun77\",\"profileImageUrl\":\"https://picsum.photos/100/318\",\"badgeOwned\":false,\"badgeType\":null,\"category\":\"자유\",\"title\":\"러닝 후 식사 타이밍\",\"content\":\"운동 후 30분~1시간 내 단백질+탄수화물 섭취가 근회복에 가장 효과적.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":9,\"commentCount\":1,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-06T20:00:00\"},\n" +
                        "  {\"tipId\":428,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"자유\",\"title\":\"러닝 동기 부여 방법 5가지\",\"content\":\"목표 대회 등록, 러닝 일지 작성, 비포어/애프터 사진, 크루 가입, 포상 설정이 효과적.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":7,\"commentCount\":0,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-06T08:00:00\"},\n" +
                        "  {\"tipId\":429,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"자유\",\"title\":\"비 오는 날 러닝복 뭐 입어야 하나\",\"content\":\"폴리에스터 계열 속건 소재가 면보다 훨씬 쾌적. 방수보다 속건이 핵심.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":5,\"commentCount\":0,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-05T19:00:00\"},\n" +
                        "  {\"tipId\":430,\"writerId\":100,\"nickname\":\"MocktestID1557\",\"profileImageUrl\":\"https://picsum.photos/100/100\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"자유\",\"title\":\"오늘도 오운완!\",\"content\":\"비 오는데도 뛰었습니다. 뿌듯해요. 내일도 화이팅!\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":2,\"commentCount\":0,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":true,\"createdAt\":\"2026-05-05T07:00:00\"}\n" +
                        "]";
            } else {
                // ALL 30개 (훈련 8 + 자유 8 + 코스 7 + 장비 7)
                json = "[\n" +
                        "  {\"tipId\":101,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"훈련\",\"title\":\"러닝 호흡법: 2:2 패턴이 최고\",\"content\":\"들숨 2보 날숨 2보 패턴으로 리듬을 잡으면 페이스 유지가 훨씬 쉬워집니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a1\"],\"likeCount\":88,\"commentCount\":14,\"liked\":false,\"bookmarked\":true,\"commented\":true,\"mine\":false,\"createdAt\":\"2026-05-17T10:00:00\"},\n" +
                        "  {\"tipId\":401,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"자유\",\"title\":\"러닝 전후 필수 스트레칭 5가지\",\"content\":\"종아리·햄스트링·고관절·발목·어깨 순서로 10초씩 풀어주세요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a2\"],\"likeCount\":120,\"commentCount\":24,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-17T09:00:00\"},\n" +
                        "  {\"tipId\":301,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"장비\",\"title\":\"초보 러너 러닝화 추천 Top 3\",\"content\":\"아식스 젤 카야노, 브룩스 고스트, 뉴발란스 1080이 쿠셔닝 좋아 입문자에게 최적.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a3\"],\"likeCount\":102,\"commentCount\":22,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-17T08:00:00\"},\n" +
                        "  {\"tipId\":201,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"코스\",\"title\":\"단국대 죽전로 야경 코스 추천\",\"content\":\"단국대 정문에서 출발해서 죽전로 한 바퀴 도는 5km 코스입니다.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=a4\",\"imageUrls\":[\"https://picsum.photos/600/600?random=a4a\"],\"likeCount\":95,\"commentCount\":18,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-17T07:00:00\"},\n" +
                        "  {\"tipId\":402,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"자유\",\"title\":\"마라톤 첫 완주 후기\",\"content\":\"42.195km 완주! 35km부터 다리가 안 풀렸지만 포기하지 않았습니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a5\"],\"likeCount\":108,\"commentCount\":21,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-16T20:00:00\"},\n" +
                        "  {\"tipId\":102,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"훈련\",\"title\":\"인터벌 트레이닝 입문 가이드\",\"content\":\"400m 전력질주 후 200m 조깅, 8세트 반복이 기본입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a6\"],\"likeCount\":72,\"commentCount\":9,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-16T10:00:00\"},\n" +
                        "  {\"tipId\":302,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"장비\",\"title\":\"GPS 워치 비교: 가민 vs 애플워치\",\"content\":\"가민은 배터리, 애플은 생태계. 순수 러닝 목적이면 가민 포어러너 추천.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a7\"],\"likeCount\":89,\"commentCount\":17,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-16T09:00:00\"},\n" +
                        "  {\"tipId\":403,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"자유\",\"title\":\"비 오는 날 러닝 꿀팁\",\"content\":\"방수 모자 + 경량 윈드브레이커면 충분. 발은 어차피 젖으니 메시 소재 신발이 편해요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":95,\"commentCount\":18,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-16T08:00:00\"},\n" +
                        "  {\"tipId\":202,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"코스\",\"title\":\"한강 반포 야경 코스 5km\",\"content\":\"반포 한강공원 주차장 → 잠수교 → 반환점 → 원점 복귀, 평탄해서 초보도 OK.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=a9\",\"imageUrls\":[\"https://picsum.photos/600/600?random=a9a\"],\"likeCount\":82,\"commentCount\":15,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-15T20:00:00\"},\n" +
                        "  {\"tipId\":103,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"훈련\",\"title\":\"템포 런으로 젖산 역치 올리기\",\"content\":\"편안한 페이스보다 15~20초 빠른 속도로 20분 달리는 게 핵심입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":61,\"commentCount\":7,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-15T10:00:00\"},\n" +
                        "  {\"tipId\":404,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"자유\",\"title\":\"러닝 뮤직 플레이리스트 공유\",\"content\":\"BPM 160~175 곡들로 구성하면 케이던스 유지에 딱 맞아요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":88,\"commentCount\":16,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-15T09:00:00\"},\n" +
                        "  {\"tipId\":303,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"장비\",\"title\":\"겨울 러닝 레이어링 완벽 가이드\",\"content\":\"베이스레이어(기모) + 미드레이어(플리스) + 방풍 아우터 3단 레이어가 정석.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":76,\"commentCount\":13,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-15T08:00:00\"},\n" +
                        "  {\"tipId\":203,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"코스\",\"title\":\"올림픽공원 순환 코스 4.7km\",\"content\":\"올림픽공원 동문 → 평화의 광장 → 몽촌토성길 → 동문, 나무가 많아 여름에도 좋아요.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=a13\",\"imageUrls\":[],\"likeCount\":74,\"commentCount\":12,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-14T18:00:00\"},\n" +
                        "  {\"tipId\":104,\"writerId\":41,\"nickname\":\"SpeedKing\",\"profileImageUrl\":\"https://picsum.photos/100/300\",\"badgeOwned\":true,\"badgeType\":\"diamond\",\"category\":\"훈련\",\"title\":\"빌드업 런: 점진적 페이스업 훈련\",\"content\":\"처음 2km는 여유롭게, 마지막 2km는 최대 속도로 마무리하는 훈련입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a14\"],\"likeCount\":55,\"commentCount\":6,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-14T09:00:00\"},\n" +
                        "  {\"tipId\":405,\"writerId\":52,\"nickname\":\"MasterPace\",\"profileImageUrl\":\"https://picsum.photos/100/352\",\"badgeOwned\":true,\"badgeType\":\"master\",\"category\":\"자유\",\"title\":\"러닝 다이어트, 진짜 효과 있나요?\",\"content\":\"주 3회 5km 6개월 기준 체지방 8% 감소. 식단 병행이 핵심입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a15\"],\"likeCount\":81,\"commentCount\":15,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-14T08:00:00\"},\n" +
                        "  {\"tipId\":304,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"장비\",\"title\":\"러닝 양말 왜 중요한가\",\"content\":\"발의 블리스터를 막으려면 면 양말 대신 기능성 메리노울 양말을 써야 해요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a16\"],\"likeCount\":64,\"commentCount\":10,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-13T09:00:00\"},\n" +
                        "  {\"tipId\":204,\"writerId\":51,\"nickname\":\"FlashRunner\",\"profileImageUrl\":\"https://picsum.photos/100/351\",\"badgeOwned\":true,\"badgeType\":\"challenger\",\"category\":\"코스\",\"title\":\"서울숲 산책 러닝 코스 3km\",\"content\":\"서울숲 1문 → 꽃사슴 방사장 → 수변 광장 → 1문 복귀, 3km 짧은 코스.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=a17\",\"imageUrls\":[\"https://picsum.photos/600/600?random=a17a\"],\"likeCount\":63,\"commentCount\":9,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-13T08:00:00\"},\n" +
                        "  {\"tipId\":105,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"훈련\",\"title\":\"5km 최단기간 완주하는 법\",\"content\":\"걷기 1분 + 달리기 2분을 반복하다가 점점 달리는 시간을 늘려가세요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a18\"],\"likeCount\":49,\"commentCount\":11,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-12T08:00:00\"},\n" +
                        "  {\"tipId\":406,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"자유\",\"title\":\"처음으로 10km 완주했어요!\",\"content\":\"3개월 전에 1km도 못 뛰었는데 오늘 10km 완주! 꾸준함이 답이에요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":74,\"commentCount\":13,\"liked\":false,\"bookmarked\":true,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-12T07:00:00\"},\n" +
                        "  {\"tipId\":305,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"장비\",\"title\":\"압박 레깅스 효과 있나요?\",\"content\":\"종아리 압박 슬리브나 압박 타이츠는 근피로 완화와 부종 방지에 실제로 효과 있습니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":55,\"commentCount\":8,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-11T09:00:00\"},\n" +
                        "  {\"tipId\":205,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"코스\",\"title\":\"남산 순환 코스 4.5km\",\"content\":\"남산도서관 → N서울타워 → 남산공원 북측 순환로, 경사 있어 칼로리 소모 많아요.\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=a21\",\"imageUrls\":[\"https://picsum.photos/600/600?random=a21a\"],\"likeCount\":57,\"commentCount\":8,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-11T08:00:00\"},\n" +
                        "  {\"tipId\":106,\"writerId\":45,\"nickname\":\"MaraFan\",\"profileImageUrl\":\"https://picsum.photos/100/320\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"훈련\",\"title\":\"다리 근력 강화 루틴 3가지\",\"content\":\"스쿼트·런지·카프레이즈를 주 3회 꾸준히 하면 러닝 속도가 올라갑니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":43,\"commentCount\":5,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-10T08:00:00\"},\n" +
                        "  {\"tipId\":407,\"writerId\":44,\"nickname\":\"NightRun77\",\"profileImageUrl\":\"https://picsum.photos/100/318\",\"badgeOwned\":false,\"badgeType\":null,\"category\":\"자유\",\"title\":\"무릎이 아플 때 러닝 어떻게 하나요?\",\"content\":\"무릎 통증 시엔 무조건 쉬어야 해요. 2주 휴식 후 천천히 재개가 원칙.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":67,\"commentCount\":12,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-10T07:00:00\"},\n" +
                        "  {\"tipId\":306,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"장비\",\"title\":\"에너지 젤 언제 먹어야 하나\",\"content\":\"장거리(10km 이상)에서 40~45분마다 1개씩, 물과 함께 섭취하세요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a24\"],\"likeCount\":47,\"commentCount\":7,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-09T09:00:00\"},\n" +
                        "  {\"tipId\":107,\"writerId\":43,\"nickname\":\"TrailWalker\",\"profileImageUrl\":\"https://picsum.photos/100/315\",\"badgeOwned\":true,\"badgeType\":\"silver\",\"category\":\"훈련\",\"title\":\"러닝 후 젖산 빠르게 제거하는 법\",\"content\":\"완전 정지보다 가벼운 조깅으로 쿨다운하면 젖산 제거 속도가 2배 빠릅니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[\"https://picsum.photos/600/600?random=a25\"],\"likeCount\":37,\"commentCount\":4,\"liked\":true,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-09T08:00:00\"},\n" +
                        "  {\"tipId\":206,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"코스\",\"title\":\"잠실 한강공원 야간 코스 6km\",\"content\":\"잠실대교 남단 → 잠실 한강공원 → 천호대교 → 복귀, 야경 최고!\",\"gpsVisible\":true,\"routeMapImageUrl\":\"https://picsum.photos/600/400?random=a26\",\"imageUrls\":[],\"likeCount\":51,\"commentCount\":7,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-09T07:00:00\"},\n" +
                        "  {\"tipId\":108,\"writerId\":44,\"nickname\":\"NightRun77\",\"profileImageUrl\":\"https://picsum.photos/100/318\",\"badgeOwned\":false,\"badgeType\":null,\"category\":\"훈련\",\"title\":\"심박수 기반 러닝 훈련법\",\"content\":\"Zone 2(최대 심박수 60~70%) 훈련이 지구력 향상에 가장 효과적입니다.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":31,\"commentCount\":3,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-08T09:00:00\"},\n" +
                        "  {\"tipId\":307,\"writerId\":44,\"nickname\":\"NightRun77\",\"profileImageUrl\":\"https://picsum.photos/100/318\",\"badgeOwned\":false,\"badgeType\":null,\"category\":\"장비\",\"title\":\"야간 러닝 반사 조끼 필수템\",\"content\":\"시인성 확보를 위해 360도 반사 조끼 필수. 저렴한 건 5천 원이면 구매 가능.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":39,\"commentCount\":5,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-08T08:00:00\"},\n" +
                        "  {\"tipId\":408,\"writerId\":42,\"nickname\":\"MorningPace\",\"profileImageUrl\":\"https://picsum.photos/100/310\",\"badgeOwned\":true,\"badgeType\":\"gold\",\"category\":\"자유\",\"title\":\"러닝 전 뭐 먹어야 할까요\",\"content\":\"2시간 전 가벼운 탄수화물(바나나, 식빵)이 최적. 공복 러닝은 지방 연소엔 좋지만 강도 높이기 어려워요.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":62,\"commentCount\":11,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":false,\"createdAt\":\"2026-05-08T07:00:00\"},\n" +
                        "  {\"tipId\":310,\"writerId\":100,\"nickname\":\"MocktestID1557\",\"profileImageUrl\":\"https://picsum.photos/100/100\",\"badgeOwned\":true,\"badgeType\":\"platinum\",\"category\":\"장비\",\"title\":\"내가 직접 작성한 장비 팁\",\"content\":\"초보자가 5:00 페이스 도전할 때 카본 플레이트 슈즈는 오히려 부상 위험.\",\"gpsVisible\":false,\"routeMapImageUrl\":null,\"imageUrls\":[],\"likeCount\":3,\"commentCount\":0,\"liked\":false,\"bookmarked\":false,\"commented\":false,\"mine\":true,\"createdAt\":\"2026-05-07T15:00:00\"}\n" +
                        "]";
            }

            // keyword 필터링: title + content 기준 (keyword 없으면 전체 통과)
            boolean tipHasKeyword = tipKeyword != null && !tipKeyword.trim().isEmpty();
            String tipLowerKeyword = tipHasKeyword ? tipKeyword.trim().toLowerCase() : "";

            java.util.List<String> tipFiltered = new java.util.ArrayList<>();
            // paginateItems와 동일한 bracket-counting 방식으로 개별 아이템 추출
            int tipDepth = 0, tipStart = -1;
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') {
                    if (tipDepth == 0) tipStart = i;
                    tipDepth++;
                } else if (c == '}') {
                    tipDepth--;
                    if (tipDepth == 0 && tipStart >= 0) {
                        String item = json.substring(tipStart, i + 1);
                        if (!tipHasKeyword) {
                            tipFiltered.add(item);
                        } else {
                            String titleVal = "", contentVal = "";
                            java.util.regex.Matcher tm = java.util.regex.Pattern.compile("\"title\":\"([^\"]*)\"").matcher(item);
                            if (tm.find()) titleVal = tm.group(1).toLowerCase();
                            java.util.regex.Matcher cm = java.util.regex.Pattern.compile("\"content\":\"([^\"]*)\"").matcher(item);
                            if (cm.find()) contentVal = cm.group(1).toLowerCase();
                            if (titleVal.contains(tipLowerKeyword) || contentVal.contains(tipLowerKeyword)) {
                                tipFiltered.add(item);
                            }
                        }
                        tipStart = -1;
                    }
                }
            }

            // page+size 슬라이싱
            int tipFrom = tipPage * tipSize;
            int tipTo   = Math.min(tipFrom + tipSize, tipFiltered.size());
            if (tipFrom >= tipFiltered.size()) return buildResponse(chain, "[]");

            StringBuilder tipSb = new StringBuilder("[");
            for (int i = tipFrom; i < tipTo; i++) {
                if (i > tipFrom) tipSb.append(",");
                tipSb.append(tipFiltered.get(i));
            }
            tipSb.append("]");
            return buildResponse(chain, tipSb.toString());
        }

        // GET /api/community/feeds — 피드 목록 조회
        if (method.equals("GET") && uri.contains("community/feeds")) {
            String json = "[\n" +
                    "  {\n" +
                    "    \"feedId\": 1, \"writerId\": 41, \"profileImageUrl\": \"https://picsum.photos/100/200\", \"nickname\": \"SpeedKing\",\n" +
                    "    \"badgeOwned\": true, \"badgeType\": \"diamond\", \"createdAt\": \"2026-05-17T18:30:00\",\n" +
                    "    \"title\": \"한강 야경 러닝 5km\", \"content\": \"오늘 한강 야경 보면서 5km 완주!\", \"taggedCount\": 2, \"likeCount\": 12, \"commentCount\": 3,\n" +
                    "    \"distance\": \"5.20km\", \"duration\": \"27:30\", \"pace\": \"5:17/km\", \"mapVisible\": true,\n" +
                    "    \"routeMapImageUri\": \"https://picsum.photos/600/400?random=1\",\n" +
                    "    \"imageUrls\": [\"https://picsum.photos/600/600?random=11\", \"https://picsum.photos/600/600?random=12\"],\n" +
                    "    \"liked\": false, \"bookmarked\": false, \"commented\": true, \"tagged\": false, \"mine\": false\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"feedId\": 2, \"writerId\": 42, \"profileImageUrl\": \"https://picsum.photos/100/210\", \"nickname\": \"MorningPace\",\n" +
                    "    \"badgeOwned\": true, \"badgeType\": \"gold\", \"createdAt\": \"2026-05-16T07:00:00\",\n" +
                    "    \"title\": \"새벽 러닝 7km 완주\", \"content\": \"비 오는 새벽이지만 그래도 뛰었습니다!\", \"taggedCount\": 0, \"likeCount\": 28, \"commentCount\": 7,\n" +
                    "    \"distance\": \"7.00km\", \"duration\": \"38:24\", \"pace\": \"5:29/km\", \"mapVisible\": true,\n" +
                    "    \"routeMapImageUri\": \"https://picsum.photos/600/400?random=2\",\n" +
                    "    \"imageUrls\": [\"https://picsum.photos/600/600?random=21\"],\n" +
                    "    \"liked\": true, \"bookmarked\": false, \"commented\": false, \"tagged\": false, \"mine\": false\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"feedId\": 3, \"writerId\": 44, \"profileImageUrl\": \"https://picsum.photos/100/220\", \"nickname\": \"NightRun77\",\n" +
                    "    \"badgeOwned\": false, \"badgeType\": null, \"createdAt\": \"2026-05-15T22:10:00\",\n" +
                    "    \"title\": \"야간 러닝 인터벌 훈련\", \"content\": \"400m × 8 인터벌 마쳤어요. 다리 후들...\", \"taggedCount\": 1, \"likeCount\": 9, \"commentCount\": 2,\n" +
                    "    \"distance\": \"4.80km\", \"duration\": \"22:15\", \"pace\": \"4:38/km\", \"mapVisible\": false,\n" +
                    "    \"routeMapImageUri\": null,\n" +
                    "    \"imageUrls\": [],\n" +
                    "    \"liked\": false, \"bookmarked\": true, \"commented\": false, \"tagged\": true, \"mine\": false\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"feedId\": 4, \"writerId\": 100, \"profileImageUrl\": null, \"nickname\": \"MocktestID1557\",\n" +
                    "    \"badgeOwned\": true, \"badgeType\": \"platinum\", \"createdAt\": \"2026-05-15T11:00:00\",\n" +
                    "    \"title\": \"오늘의 코스 추천\", \"content\": \"단국대 근처 죽전로 코스 진짜 좋아요\", \"taggedCount\": 3, \"likeCount\": 5, \"commentCount\": 1,\n" +
                    "    \"distance\": \"3.20km\", \"duration\": \"18:00\", \"pace\": \"5:37/km\", \"mapVisible\": true,\n" +
                    "    \"routeMapImageUri\": \"https://picsum.photos/600/400?random=4\",\n" +
                    "    \"imageUrls\": [\"https://picsum.photos/600/600?random=41\"],\n" +
                    "    \"liked\": true, \"bookmarked\": true, \"commented\": true, \"tagged\": false, \"mine\": true\n" +
                    "  }\n" +
                    "]";
            return buildResponse(chain, json);
        }

        // PUT /api/community/feeds/{id} — 피드 수정
        if (method.equals("PUT") && uri.matches(".*community/feeds/\\d+.*")) {
            String feedId = uri.replaceAll(".*community/feeds/(\\d+).*", "$1");
            String json = "{\n" +
                    "  \"feedId\": " + feedId + ", \"writerId\": 100, \"profileImageUrl\": null, \"nickname\": \"MocktestID1557\",\n" +
                    "  \"createdAt\": \"2026-05-17T12:00:00\", \"title\": \"수정된 피드 (mock)\", \"content\": \"수정 반영된 mock 피드\",\n" +
                    "  \"taggedCount\": 0, \"likeCount\": 0, \"commentCount\": 0,\n" +
                    "  \"distance\": \"0.50km\", \"duration\": \"02:52\", \"pace\": \"5:44/km\", \"mapVisible\": false,\n" +
                    "  \"routeMapImageUri\": null, \"imageUrls\": [],\n" +
                    "  \"badgeOwned\": true, \"badgeType\": \"platinum\",\n" +
                    "  \"liked\": false, \"bookmarked\": false, \"commented\": false, \"tagged\": false\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // POST /api/community/feeds — 피드 업로드
        if (method.equals("POST") && uri.contains("community/feeds")) {
            String json = "{\n" +
                    "  \"feedId\": 9999, \"writerId\": 100, \"profileImageUrl\": null, \"nickname\": \"MocktestID1557\",\n" +
                    "  \"createdAt\": \"2026-05-17T11:30:00\", \"title\": \"새로 업로드한 피드 (mock)\", \"content\": \"방금 업로드된 mock 피드\",\n" +
                    "  \"taggedCount\": 0, \"likeCount\": 0, \"commentCount\": 0,\n" +
                    "  \"distance\": \"0.50km\", \"duration\": \"02:52\", \"pace\": \"5:44/km\", \"mapVisible\": false,\n" +
                    "  \"routeMapImageUri\": null, \"imageUrls\": []\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // =====================================================
        // ▼▼▼ 커뮤니티 팁 API (TipApi 매핑) ▼▼▼
        // =====================================================

        // POST /api/community/tips/{id}/likes — 팁 좋아요 토글 (실제 상태 토글)
        if (method.equals("POST") && uri.matches(".*community/tips/\\d+/likes.*")) {
            String tipId = uri.replaceAll(".*community/tips/(\\d+)/likes.*", "$1");
            boolean current = Boolean.TRUE.equals(tipLikeState.get(tipId));
            boolean newLiked = !current;
            tipLikeState.put(tipId, newLiked);
            int baseCount = 22;
            String json = "{\"tipId\": " + tipId + ", \"liked\": " + newLiked
                    + ", \"likeCount\": " + (baseCount + (newLiked ? 1 : 0)) + "}";
            return buildResponse(chain, json);
        }

        // POST /api/community/tips/{id}/bookmarks — 팁 북마크 토글 (실제 상태 토글)
        if (method.equals("POST") && uri.matches(".*community/tips/\\d+/bookmarks.*")) {
            String tipId = uri.replaceAll(".*community/tips/(\\d+)/bookmarks.*", "$1");
            boolean current = Boolean.TRUE.equals(tipBookmarkState.get(tipId));
            boolean newBookmarked = !current;
            tipBookmarkState.put(tipId, newBookmarked);
            String json = "{\"tipId\": " + tipId + ", \"bookmarked\": " + newBookmarked + "}";
            return buildResponse(chain, json);
        }

        // PUT /api/community/tips/{id}/comments/{commentId} — 팁 댓글 수정
        if (method.equals("PUT") && uri.matches(".*community/tips/\\d+/comments/\\d+.*")) {
            String commentId = uri.replaceAll(".*comments/(\\d+).*", "$1");
            String json = "{\n" +
                    "  \"commentId\": " + commentId + ",\n" +
                    "  \"writerId\": 100,\n" +
                    "  \"nickname\": \"MocktestID1557\",\n" +
                    "  \"profileImageUrl\": null,\n" +
                    "  \"badgeOwned\": true,\n" +
                    "  \"badgeType\": \"platinum\",\n" +
                    "  \"content\": \"수정된 댓글 (mock)\",\n" +
                    "  \"createdAt\": \"2026-05-18T11:00:00\",\n" +
                    "  \"mine\": true\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // POST /api/community/tips/{id}/comments — 팁 댓글 작성
        if (method.equals("POST") && uri.matches(".*community/tips/\\d+/comments.*")) {
            String json = "{\n" +
                    "  \"commentId\": 8888,\n" +
                    "  \"writerId\": 100,\n" +
                    "  \"nickname\": \"MocktestID1557\",\n" +
                    "  \"profileImageUrl\": null,\n" +
                    "  \"badgeOwned\": true,\n" +
                    "  \"badgeType\": \"platinum\",\n" +
                    "  \"content\": \"좋은 팁 감사합니다! (mock)\",\n" +
                    "  \"createdAt\": \"2026-05-17T11:00:00\",\n" +
                    "  \"mine\": true\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // GET /api/community/tips/{id} — 팁 상세 조회
        if (method.equals("GET") && uri.matches(".*community/tips/\\d+.*")) {
            String tipId = uri.replaceAll(".*community/tips/(\\d+).*", "$1");
            String json = "{\n" +
                    "  \"tipId\": " + tipId + ",\n" +
                    "  \"writerId\": 41,\n" +
                    "  \"nickname\": \"SpeedKing\",\n" +
                    "  \"profileImageUrl\": \"https://picsum.photos/100/300\",\n" +
                    "  \"badgeOwned\": true,\n" +
                    "  \"badgeType\": \"diamond\",\n" +
                    "  \"category\": \"훈련\",\n" +
                    "  \"title\": \"러닝 호흡법: 2:2 패턴이 최고\",\n" +
                    "  \"content\": \"입으로 들이마시고 입으로 내쉬는 2:2 호흡 패턴을 추천합니다. 4걸음에 한 번 호흡 사이클을 맞추면 페이스가 안정됩니다. 처음엔 어색하지만 일주일만 의식적으로 연습하면 자연스러워져요.\",\n" +
                    "  \"gpsVisible\": false,\n" +
                    "  \"routeMapImageUrl\": null,\n" +
                    "  \"imageUrls\": [\"https://picsum.photos/600/600?random=t" + tipId + "1\"],\n" +
                    "  \"likeCount\": 22,\n" +
                    "  \"commentCount\": 4,\n" +
                    "  \"liked\": false,\n" +
                    "  \"bookmarked\": true,\n" +
                    "  \"mine\": false,\n" +
                    "  \"createdAt\": \"2026-05-15T10:00:00\",\n" +
                    "  \"comments\": [\n" +
                    "    {\"commentId\": 1, \"writerId\": 42, \"nickname\": \"MorningPace\", \"profileImageUrl\": \"https://picsum.photos/100/301\", \"badgeOwned\": true, \"badgeType\": \"gold\", \"content\": \"오 시도해볼게요!\", \"createdAt\": \"2026-05-15T10:30:00\", \"mine\": false},\n" +
                    "    {\"commentId\": 2, \"writerId\": 43, \"nickname\": \"TrailWalker\", \"profileImageUrl\": null, \"badgeOwned\": true, \"badgeType\": \"bronze\", \"content\": \"저는 3:2가 편하던데 사람마다 다른 듯\", \"createdAt\": \"2026-05-15T11:00:00\", \"mine\": false},\n" +
                    "    {\"commentId\": 3, \"writerId\": 100, \"nickname\": \"MocktestID1557\", \"profileImageUrl\": null, \"badgeOwned\": true, \"badgeType\": \"platinum\", \"content\": \"감사합니다 도움 됐어요\", \"createdAt\": \"2026-05-15T12:00:00\", \"mine\": true},\n" +
                    "    {\"commentId\": 4, \"writerId\": 44, \"nickname\": \"NightRun77\", \"profileImageUrl\": \"https://picsum.photos/100/302\", \"badgeOwned\": false, \"badgeType\": null, \"content\": \"좋은 팁이네요\", \"createdAt\": \"2026-05-15T13:00:00\", \"mine\": false}\n" +
                    "  ]\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // GET /api/community/tips/me — 내가 쓴 팁 목록 조회
        if (method.equals("GET") && uri.contains("community/tips/me")) {
            String json = "[\n" +
                    "  {\n" +
                    "    \"tipId\": 4, \"writerId\": 100, \"nickname\": \"MocktestID1557\", \"profileImageUrl\": \"https://picsum.photos/100/100\",\n" +
                    "    \"badgeOwned\": true, \"badgeType\": \"platinum\", \"category\": \"장비\",\n" +
                    "    \"title\": \"내가 직접 작성한 팁\", \"content\": \"초보자가 5:00 페이스 도전할 때 팁: 처음 1km는 일부러 6:00 정도로 천천히 출발하세요.\",\n" +
                    "    \"gpsVisible\": false, \"routeMapImageUrl\": null,\n" +
                    "    \"imageUrls\": [],\n" +
                    "    \"likeCount\": 3, \"commentCount\": 0,\n" +
                    "    \"liked\": false, \"bookmarked\": false, \"commented\": false, \"mine\": true, \"createdAt\": \"2026-05-12T15:00:00\"\n" +
                    "  }\n" +
                    "]";
            return buildResponse(chain, json);
        }

        // GET /api/community/tips — 팁 목록 조회
        if (method.equals("GET") && uri.contains("community/tips")) {
            String json = "[\n" +
                    "  {\n" +
                    "    \"tipId\": 1, \"writerId\": 41, \"nickname\": \"SpeedKing\", \"profileImageUrl\": \"https://picsum.photos/100/300\",\n" +
                    "    \"badgeOwned\": true, \"badgeType\": \"diamond\", \"category\": \"훈련\",\n" +
                    "    \"title\": \"러닝 호흡법: 2:2 패턴이 최고\", \"content\": \"입으로 들이마시고 입으로 내쉬는 2:2 호흡 패턴을 추천합니다.\",\n" +
                    "    \"gpsVisible\": false, \"routeMapImageUrl\": null,\n" +
                    "    \"imageUrls\": [\"https://picsum.photos/600/600?random=t11\"],\n" +
                    "    \"likeCount\": 22, \"commentCount\": 4,\n" +
                    "    \"liked\": false, \"bookmarked\": true, \"commented\": true, \"mine\": false, \"createdAt\": \"2026-05-15T10:00:00\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"tipId\": 2, \"writerId\": 42, \"nickname\": \"MorningPace\", \"profileImageUrl\": \"https://picsum.photos/100/310\",\n" +
                    "    \"badgeOwned\": true, \"badgeType\": \"gold\", \"category\": \"자유\",\n" +
                    "    \"title\": \"러닝 전후 필수 스트레칭 5가지\", \"content\": \"종아리·햄스트링·고관절·발목·어깨 순서로 풀어주세요. 시간 없으면 종아리만이라도!\",\n" +
                    "    \"gpsVisible\": false, \"routeMapImageUrl\": null,\n" +
                    "    \"imageUrls\": [\"https://picsum.photos/600/600?random=t21\", \"https://picsum.photos/600/600?random=t22\"],\n" +
                    "    \"likeCount\": 45, \"commentCount\": 12,\n" +
                    "    \"liked\": true, \"bookmarked\": false, \"commented\": false, \"mine\": false, \"createdAt\": \"2026-05-14T20:00:00\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"tipId\": 3, \"writerId\": 45, \"nickname\": \"MaraFan\", \"profileImageUrl\": \"https://picsum.photos/100/320\",\n" +
                    "    \"badgeOwned\": true, \"badgeType\": \"platinum\", \"category\": \"코스\",\n" +
                    "    \"title\": \"단국대 죽전로 야경 코스 추천\", \"content\": \"단국대 정문에서 출발해서 죽전로 한 바퀴 도는 5km 코스 진짜 좋아요. 가로등도 충분하고 안전합니다.\",\n" +
                    "    \"gpsVisible\": true, \"routeMapImageUrl\": \"https://picsum.photos/600/400?random=t31\",\n" +
                    "    \"imageUrls\": [\"https://picsum.photos/600/600?random=t31a\"],\n" +
                    "    \"likeCount\": 38, \"commentCount\": 8,\n" +
                    "    \"liked\": false, \"bookmarked\": false, \"commented\": false, \"mine\": false, \"createdAt\": \"2026-05-13T19:00:00\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"tipId\": 4, \"writerId\": 100, \"nickname\": \"MocktestID1557\", \"profileImageUrl\": \"https://picsum.photos/100/100\",\n" +
                    "    \"badgeOwned\": true, \"badgeType\": \"platinum\", \"category\": \"장비\",\n" +
                    "    \"title\": \"내가 직접 작성한 팁\", \"content\": \"초보자가 5:00 페이스 도전할 때 팁: 처음 1km는 일부러 6:00 정도로 천천히 출발하세요.\",\n" +
                    "    \"gpsVisible\": false, \"routeMapImageUrl\": null,\n" +
                    "    \"imageUrls\": [],\n" +
                    "    \"likeCount\": 3, \"commentCount\": 0,\n" +
                    "    \"liked\": false, \"bookmarked\": false, \"commented\": false, \"mine\": true, \"createdAt\": \"2026-05-12T15:00:00\"\n" +
                    "  }\n" +
                    "]";
            return buildResponse(chain, json);
        }

        // PUT /api/community/tips/{id} — 팁 수정
        if (method.equals("PUT") && uri.matches(".*community/tips/\\d+.*")) {
            String tipId = uri.replaceAll(".*community/tips/(\\d+).*", "$1");
            String json = "{\n" +
                    "  \"tipId\": " + tipId + ", \"writerId\": 100, \"nickname\": \"MocktestID1557\", \"profileImageUrl\": null,\n" +
                    "  \"badgeOwned\": false, \"badgeType\": null, \"category\": \"자유\",\n" +
                    "  \"title\": \"수정된 팁 (mock)\", \"content\": \"수정 반영된 mock 팁\",\n" +
                    "  \"gpsVisible\": false, \"routeMapImageUrl\": null, \"imageUrls\": [],\n" +
                    "  \"likeCount\": 0, \"commentCount\": 0, \"liked\": false, \"bookmarked\": false,\n" +
                    "  \"createdAt\": \"2026-05-17T12:00:00\"\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // POST /api/community/tips — 팁 업로드
        if (method.equals("POST") && uri.contains("community/tips")) {
            String json = "{\n" +
                    "  \"tipId\": 9999, \"writerId\": 100, \"nickname\": \"MocktestID1557\", \"profileImageUrl\": null,\n" +
                    "  \"badgeOwned\": false, \"badgeType\": null, \"category\": \"자유\",\n" +
                    "  \"title\": \"새로 업로드한 팁 (mock)\", \"content\": \"방금 업로드된 mock 팁\",\n" +
                    "  \"gpsVisible\": false, \"routeMapImageUrl\": null, \"imageUrls\": [],\n" +
                    "  \"likeCount\": 0, \"commentCount\": 0, \"liked\": false, \"bookmarked\": false,\n" +
                    "  \"createdAt\": \"2026-05-17T11:30:00\"\n" +
                    "}";
            return buildResponse(chain, json);
        }

        if (method.equals("DELETE")) return buildResponse(chain, "{\"status\": \"success\"}");


        // 4. [마이페이지 프로필 관련] - GET(조회)와 PUT(수정) 분기 처리
        if (uri.contains("users/me/profile")) {
            if (method.equals("PUT")) {
                // 수정 요청이 들어왔을 때의 성공 응답
                return buildResponse(chain, "{\"message\":\"success\"}");
            } else {
                // [수정된 부분] 조회(GET) 요청일 때 새로운 카운트 데이터 추가
                String json = "{\n" +
                        "  \"community_profile_name\": \"MocktestID1557\",\n" +
                        "  \"profile_photo\": \"https://picsum.photos/100/100\",\n" +
                        "  \"status_message\": \"항상 열심히 운동하자!\",\n" +
                        "  \"friend_count\": 15,\n" +
                        "  \"post_count\": 2,\n" +
                        "  \"tip_count\": 1,\n" +
                        "  \"tagged_count\": 5,\n" +
                        "  \"commented_feed_count\": 8,\n" +
                        "  \"liked_feed_count\": 0,\n" +
                        "  \"bookmarked_feed_count\": 3\n" +
                        "}";
                return buildResponse(chain, json);
            }
        }

        // [특정 유저 친구 목록] community/friends/user/{userId} — 반드시 일반 friends 조건보다 먼저 체크
        // status = 로그인한 내가 해당 사람과의 관계 (friends/sent/none 등)
        if (uri.contains("community/friends/user/")) {
            String json = "[\n" +
                    "  {\"user_id\": 41, \"nickname\": \"SpeedKing\",   \"badge_tier\": \"diamond\",  \"friend_count\": 42, \"profile_image_url\": \"https://picsum.photos/100/141\", \"status\": \"friends\"},\n" +
                    "  {\"user_id\": 42, \"nickname\": \"MorningPace\", \"badge_tier\": \"gold\",     \"friend_count\": 18, \"profile_image_url\": \"https://picsum.photos/100/142\", \"status\": \"none\"},\n" +
                    "  {\"user_id\": 43, \"nickname\": \"TrailWalker\", \"badge_tier\": \"silver\",   \"friend_count\": 7,  \"profile_image_url\": null,                             \"status\": \"sent\"},\n" +
                    "  {\"user_id\": 44, \"nickname\": \"NightRun77\",  \"badge_tier\": \"bronze\",   \"friend_count\": 3,  \"profile_image_url\": \"https://picsum.photos/100/144\", \"status\": \"friends\"},\n" +
                    "  {\"user_id\": 45, \"nickname\": \"MaraFan\",     \"badge_tier\": \"platinum\", \"friend_count\": 56, \"profile_image_url\": \"https://picsum.photos/100/145\", \"status\": \"none\"}\n" +
                    "]";
            return buildResponse(chain, json);
        }

        // [친구 목록] community/friends?status=...
        if (uri.contains("community/friends") && !uri.contains("action")) {
            String json;
            if (uri.contains("status=sent")) {
                json = "[\n" +
                        "  {\"user_id\": 11, \"nickname\": \"SentUser1\", \"badge_tier\": \"silver\", \"friend_count\": 8, \"profile_image_url\": \"https://picsum.photos/100/111\", \"status\": \"sent\"},\n" +
                        "  {\"user_id\": 12, \"nickname\": \"SentUser2\", \"badge_tier\": \"none\",   \"friend_count\": 2, \"profile_image_url\": null,                           \"status\": \"sent\"}\n" +
                        "]";
            } else if (uri.contains("status=received")) {
                json = "[\n" +
                        "  {\"user_id\": 21, \"nickname\": \"ReceivedUser1\", \"badge_tier\": \"gold\",     \"friend_count\": 20, \"profile_image_url\": \"https://picsum.photos/100/121\", \"status\": \"received\"},\n" +
                        "  {\"user_id\": 22, \"nickname\": \"ReceivedUser2\", \"badge_tier\": \"bronze\",   \"friend_count\": 5,  \"profile_image_url\": \"https://picsum.photos/100/122\", \"status\": \"received\"},\n" +
                        "  {\"user_id\": 23, \"nickname\": \"ReceivedUser3\", \"badge_tier\": \"platinum\", \"friend_count\": 33, \"profile_image_url\": null,                            \"status\": \"received\"}\n" +
                        "]";
            } else if (uri.contains("status=blocked")) {
                json = "[\n" +
                        "  {\"user_id\": 31, \"nickname\": \"BlockedUser1\", \"badge_tier\": \"none\",   \"friend_count\": 1, \"profile_image_url\": null,                           \"status\": \"blocked\"},\n" +
                        "  {\"user_id\": 32, \"nickname\": \"BlockedUser2\", \"badge_tier\": \"silver\", \"friend_count\": 7, \"profile_image_url\": \"https://picsum.photos/100/131\", \"status\": \"blocked\"}\n" +
                        "]";
            } else {
                // status=friends (기본)
                json = "[\n" +
                        "  {\"user_id\": 1, \"nickname\": \"RunnerA\",    \"badge_tier\": \"gold\",     \"friend_count\": 3,  \"profile_image_url\": \"https://picsum.photos/100/102\", \"status\": \"friends\"},\n" +
                        "  {\"user_id\": 2, \"nickname\": \"HealthMania\", \"badge_tier\": \"silver\",   \"friend_count\": 12, \"profile_image_url\": \"https://picsum.photos/100/103\", \"status\": \"friends\"},\n" +
                        "  {\"user_id\": 3, \"nickname\": \"NatureRun\",   \"badge_tier\": \"bronze\",   \"friend_count\": 5,  \"profile_image_url\": null,                            \"status\": \"friends\"},\n" +
                        "  {\"user_id\": 4, \"nickname\": \"RouteMaster\", \"badge_tier\": \"diamond\",  \"friend_count\": 28, \"profile_image_url\": \"https://picsum.photos/100/104\", \"status\": \"friends\"}\n" +
                        "]";
            }
            return buildResponse(chain, json);
        }

        // 5-0. [러너페이지 피드] community/contents/user/{userId} → 반드시 /me, /tagged 등보다 먼저 체크
        if (uri.contains("community/contents/user/")) {
            String json = "[\n" +
                    "  {\n" +
                    "    \"content_id\": 601,\n" +
                    "    \"user_id\": 999,\n" +
                    "    \"content_title\": \"오운완\",\n" +
                    "    \"content_text\": \"오늘도 열심히 뛰었습니다!\",\n" +
                    "    \"profile_image_url\": \"https://picsum.photos/100/102\",\n" +
                    "    \"nickname\": \"RunnerA\",\n" +
                    "    \"badge_tier\": \"gold\",\n" +
                    "    \"is_bookmarked\": false,\n" +
                    "    \"is_tagged\": false,\n" +
                    "    \"is_liked\": false,\n" +
                    "    \"is_commented\": false,\n" +
                    "    \"image_url\": \"https://picsum.photos/400/402\",\n" +
                    "    \"route_map_url\": \"https://picsum.photos/300/151\",\n" +
                    "    \"tag_count\": 2,\n" +
                    "    \"like_count\": 20,\n" +
                    "    \"comment_count\": 3,\n" +
                    "    \"total_distance\": 5.5,\n" +
                    "    \"duration\": 1200,\n" +
                    "    \"pace\": 360,\n" +
                    "    \"created_at\": \"2026-05-01T10:00:00\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"content_id\": 602,\n" +
                    "    \"user_id\": 999,\n" +
                    "    \"content_title\": \"유산소 빡세게 한 날\",\n" +
                    "    \"content_text\": \"힘들었지만 뿌듯해요.\",\n" +
                    "    \"profile_image_url\": \"https://picsum.photos/100/102\",\n" +
                    "    \"nickname\": \"RunnerA\",\n" +
                    "    \"badge_tier\": \"gold\",\n" +
                    "    \"is_bookmarked\": false,\n" +
                    "    \"is_tagged\": false,\n" +
                    "    \"is_liked\": false,\n" +
                    "    \"is_commented\": false,\n" +
                    "    \"image_url\": \"https://picsum.photos/400/410\",\n" +
                    "    \"route_map_url\": null,\n" +
                    "    \"tag_count\": 1,\n" +
                    "    \"like_count\": 15,\n" +
                    "    \"comment_count\": 6,\n" +
                    "    \"total_distance\": 22.3,\n" +
                    "    \"duration\": 9158,\n" +
                    "    \"pace\": 410,\n" +
                    "    \"created_at\": \"2026-03-02T09:00:00\"\n" +
                    "  }\n" +
                    "]";
            return buildResponse(chain, json);
        }

        // 5. [마이페이지 피드 목록 통합 관리]
        if (uri.contains("community/contents")) {
            String json = ""; // 결과를 담을 변수 선언

            if (uri.endsWith("/me")) {
                // 내가 쓴 피드 (이미지, 통계 데이터 추가 버전)
                json = "[\n" +
                        "  {\n" +
                        "    \"content_id\": 101,\n" +
                        "    \"user_id\": 100,\n" +
                        "    \"content_title\": \"오늘의 오운완 기록!\",\n" +
                        "    \"content_text\": \"오운완! 오늘 코스는 한강변이었어요.\",\n" +
                        "    \"profile_image_url\": \"https://picsum.photos/100/100\",\n" +
                        "    \"nickname\": \"MocktestID1557\",\n" +
                        "    \"badge_tier\": \"platinum\",\n" +
                        "    \"is_bookmarked\": true,\n" +
                        "    \"is_tagged\": false,\n" +
                        "    \"is_liked\": false,\n" +     // [추가] 하이라이트 OFF
                        "    \"is_commented\": true,\n" +  // [추가] 하이라이트 ON
                        "    \"image_url\": \"https://picsum.photos/400/400\",\n" +
                        "    \"route_map_url\": \"https://picsum.photos/300/150\",\n" +
                        "    \"tag_count\": 3,\n" +
                        "    \"like_count\": 12,\n" +
                        "    \"comment_count\": 5,\n" +
                        "    \"total_distance\": 11.8,\n" +
                        "    \"duration\": 2269,\n" +
                        "    \"pace\": 384,\n" +
                        "    \"created_at\": \"2026-05-05T17:10:00\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"content_id\": 102,\n" +
                        "    \"user_id\": 100,\n" +
                        "    \"content_title\": \"호에엥\",\n" +
                        "    \"content_text\": \"유산소 빡세게 한 날... 힘들다.\",\n" +
                        "    \"profile_image_url\": \"https://picsum.photos/100/100\",\n" +
                        "    \"nickname\": \"MocktestID1557\",\n" +
                        "    \"badge_tier\": \"platinum\",\n" +
                        "    \"is_bookmarked\": false,\n" +
                        "    \"is_tagged\": false,\n" +
                        "    \"is_liked\": true,\n" +     // [추가] 하이라이트 OFF
                        "    \"is_commented\": true,\n" +  // [추가] 하이라이트 ON
                        "    \"image_url\": \"https://picsum.photos/400/401\",\n" +
                        "    \"route_map_url\": null,\n" +
                        "    \"tag_count\": 2,\n" +
                        "    \"like_count\": 45,\n" +
                        "    \"comment_count\": 10,\n" +
                        "    \"total_distance\": 22.3,\n" +
                        "    \"duration\": 9158,\n" +
                        "    \"pace\": 410,\n" +
                        "    \"created_at\": \"2026-03-02T09:00:00\"\n" +
                        "  }\n" +
                        "]";
            }


            else if (uri.endsWith("/tagged")) {
                // [나를 태그한 피드] 태그만 되고 아직 반응은 안 한 상태 테스트
                json = "[\n" +
                        "  {\n" +
                        "    \"content_id\": 201,\n" +
                        "    \"content_title\": \"러닝 크루 정기런\",\n" +
                        "    \"content_text\": \"@MocktestID1557 님과 함께 달렸어요! 오늘 페이스 좋네요.\",\n" +
                        "    \"profile_image_url\": \"https://picsum.photos/100/102\",\n" +
                        "    \"nickname\": \"RunnerA\",\n" +
                        "    \"badge_tier\": \"gold\",\n" +
                        "    \"is_bookmarked\": false,\n" +
                        "    \"is_tagged\": true,\n" +
                        "    \"is_liked\": false,\n" +     // [추가] 하이라이트 OFF
                        "    \"is_commented\": false,\n" + // [추가] 하이라이트 OFF
                        "    \"image_url\": \"https://picsum.photos/400/402\",\n" +
                        "    \"route_map_url\": \"https://picsum.photos/300/151\",\n" +
                        "    \"tag_count\": 5,\n" +
                        "    \"like_count\": 20,\n" +
                        "    \"comment_count\": 8,\n" +
                        "    \"total_distance\": 5.5,\n" +
                        "    \"duration\": 1200,\n" +
                        "    \"pace\": 360,\n" +
                        "    \"created_at\": \"2026-05-01T10:00:00\"\n" +
                        "  }\n" +
                        "]";
            }
            else if (uri.endsWith("/comments")) {
                // [내가 댓글 단 피드] 댓글은 썼지만 좋아요는 안 누른 상태 테스트
                json = "[\n" +
                        "  {\n" +
                        "    \"content_id\": 301,\n" +
                        "    \"content_title\": \"같이 달리실 분?\",\n" +
                        "    \"content_text\": \"댓글 단 피드 예시입니다. 일요일 아침 남산 코스 어떠세요?\",\n" +
                        "    \"profile_image_url\": \"https://picsum.photos/100/103\",\n" +
                        "    \"nickname\": \"HealthMania\",\n" +
                        "    \"badge_tier\": \"silver\",\n" +
                        "    \"is_bookmarked\": false,\n" +
                        "    \"is_tagged\": true,\n" +
                        "    \"is_liked\": false,\n" +     // [추가] 하이라이트 OFF
                        "    \"is_commented\": true,\n" +  // [추가] 하이라이트 ON
                        "    \"image_url\": null,\n" +
                        "    \"route_map_url\": null,\n" +
                        "    \"tag_count\": 1,\n" +
                        "    \"like_count\": 8,\n" +
                        "    \"comment_count\": 15,\n" +
                        "    \"total_distance\": 8.2,\n" +
                        "    \"duration\": 2400,\n" +
                        "    \"pace\": 400,\n" +
                        "    \"created_at\": \"2026-04-20T14:00:00\"\n" +
                        "  }\n" +
                        "]";
            }
            else if (uri.endsWith("/likes")) {
                // [내가 좋아요 한 피드] 좋아요는 눌렀지만 댓글은 안 단 상태 테스트
                json = "[\n" +
                        "  {\n" +
                        "    \"content_id\": 401,\n" +
                        "    \"content_title\": \"좋아요를 누른 멋진 피드\",\n" +
                        "    \"content_text\": \"풍경이 너무 예뻐서 좋아요 누를 수밖에 없었네요.\",\n" +
                        "    \"profile_image_url\": \"https://picsum.photos/100/104\",\n" +
                        "    \"nickname\": \"NatureRunner\",\n" +
                        "    \"badge_tier\": \"bronze\",\n" +
                        "    \"is_bookmarked\": false,\n" +
                        "    \"is_tagged\": true,\n" +
                        "    \"is_liked\": true,\n" +      // [추가] 하이라이트 ON
                        "    \"is_commented\": false,\n" + // [추가] 하이라이트 OFF
                        "    \"image_url\": \"https://picsum.photos/400/403\",\n" +
                        "    \"route_map_url\": \"https://picsum.photos/300/152\",\n" +
                        "    \"tag_count\": 0,\n" +
                        "    \"like_count\": 150,\n" +
                        "    \"comment_count\": 20,\n" +
                        "    \"total_distance\": 10.0,\n" +
                        "    \"duration\": 3000,\n" +
                        "    \"pace\": 300,\n" +
                        "    \"created_at\": \"2026-04-15T09:00:00\"\n" +
                        "  }\n" +
                        "]";
            }
            else if (uri.endsWith("/bookmarks")) {
                // [내가 북마크 한 피드] 북마크만 하고 다른 반응은 없는 상태 테스트
                json = "[\n" +
                        "  {\n" +
                        "    \"content_id\": 501,\n" +
                        "    \"content_title\": \"나중에 따라 뛸 코스\",\n" +
                        "    \"content_text\": \"나중에 다시 볼 북마크 피드입니다.\",\n" +
                        "    \"profile_image_url\": \"https://picsum.photos/100/105\",\n" +
                        "    \"nickname\": \"RouteMaster\",\n" +
                        "    \"badge_tier\": \"diamond\",\n" +
                        "    \"is_bookmarked\": true,\n" +
                        "    \"is_tagged\": false,\n" +
                        "    \"is_liked\": false,\n" +
                        "    \"is_commented\": false,\n" +
                        "    \"image_url\": \"https://picsum.photos/400/404\",\n" +
                        "    \"route_map_url\": \"https://picsum.photos/300/153\",\n" +
                        "    \"tag_count\": 2,\n" +
                        "    \"like_count\": 55,\n" +
                        "    \"comment_count\": 12,\n" +
                        "    \"total_distance\": 15.3,\n" +
                        "    \"duration\": 4500,\n" +
                        "    \"pace\": 350,\n" +
                        "    \"created_at\": \"2026-04-10T20:00:00\"\n" +
                        "  }\n" +
                        "]";
            }

            return buildResponse(chain, json);
        }


        // Mockserver 안에서 프로필 수정(PUT) 응답
        if (method.equals("DELETE")) return buildResponse(chain, "{\"status\": \"success\"}");


        // 4-1. [러너페이지 프로필] users/{userId}/profile (me가 아닌 경우)
        // is_friend: true  → 친구 삭제 버튼
        // is_friend: false → 친구요청 버튼
        if (uri.matches(".*users/\\d+/profile.*")) {
            String json = "{\n" +
                    "  \"community_profile_name\": \"RunnerA\",\n" +
                    "  \"profile_photo\": \"https://picsum.photos/200/200\",\n" +
                    "  \"status_message\": \"매일 조금씩 더 멀리!\",\n" +
                    "  \"friend_count\": 3,\n" +
                    "  \"post_count\": 2,\n" +
                    "  \"is_friend\": false\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // 6. [배지 상세 정보 조회]
        if (uri.contains("users/me/badge")) {
            String json = "{\n" +
                    "  \"Badge\": \"platinum\",\n" +
                    "  \"record_id\": 105, \n" +          // 배지를 획득하게 한 특정 기록의 ID
                    "  \"distance\": 42.19,\n" +
                    "  \"pace\": \"3:55\",\n" +
                    "  \"achieved_at\": \"2026-05-08\"\n" +
                    "}";
            return buildResponse(chain, json);
        }


        // 6-1. [러너페이지 배지] users/{userId}/badge (me가 아닌 경우)
        if (uri.matches(".*users/\\d+/badge.*")) {
            String json = "{\n" +
                    "  \"Badge\": \"gold\",\n" +
                    "  \"record_id\": 601,\n" +
                    "  \"distance\": 5.5,\n" +
                    "  \"pace\": \"6:00\",\n" +
                    "  \"achieved_at\": \"2026-05-01\"\n" +
                    "}";
            return buildResponse(chain, json);
        }

        return chain.proceed(chain.request());
    }

    /*
     * nickname 필드 기준으로 keyword 필터링 후 page+size 슬라이싱하는 함수임
     * keyword 없으면 전체 통과, 있으면 nickname에 keyword 포함된 항목만 반환함
     */
    private String filterByNickname(String jsonArray, String keyword, int page, int size) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String lowerKeyword = hasKeyword ? keyword.trim().toLowerCase() : "";

        java.util.List<String> filtered = new java.util.ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    String item = jsonArray.substring(start, i + 1);
                    if (!hasKeyword) {
                        filtered.add(item);
                    } else {
                        java.util.regex.Matcher m = java.util.regex.Pattern
                                .compile("\"nickname\":\\s*\"([^\"]*)\"").matcher(item);
                        if (m.find() && m.group(1).toLowerCase().contains(lowerKeyword)) {
                            filtered.add(item);
                        }
                    }
                    start = -1;
                }
            }
        }

        int from = page * size;
        int to   = Math.min(from + size, filtered.size());
        if (from >= filtered.size()) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = from; i < to; i++) {
            if (i > from) sb.append(",");
            sb.append(filtered.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    /*
     * JSON 배열 문자열에서 개별 객체를 추출해 page+size 기반으로 슬라이싱 후 반환하는 함수임
     * 중첩 중괄호를 카운팅해 JSON 파싱 없이 안전하게 아이템을 분리함
     */
    private String paginateItems(String jsonArray, int page, int size) {
        java.util.List<String> items = new java.util.ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    items.add(jsonArray.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        int from = page * size;
        int to   = Math.min(from + size, items.size());

        if (from >= items.size()) return "[]";

        StringBuilder sb = new StringBuilder("[");
        java.util.List<String> slice = items.subList(from, to);
        for (int i = 0; i < slice.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(slice.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private Response buildResponse(Chain chain, String json) {
        return new Response.Builder()
                .code(200)
                .message("OK")
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .body(ResponseBody.create(MediaType.parse("application/json"), json))
                .addHeader("content-type", "application/json")
                .build();
    }
}
