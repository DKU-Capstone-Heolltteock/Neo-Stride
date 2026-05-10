package com.neostride.app.common.network;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.io.IOException;

public class Mockserver implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        String uri = chain.request().url().uri().toString();
        String method = chain.request().method();

        // 1. [러닝 기록] plan_id 통일 (GSON 모델 매칭)
        if (uri.contains("records")) {
            String json = "[\n" +
                    "  {\n" +
                    "    \"plan_id\": null,\n" +
                    "    \"created_at\": \"2026-05-03T07:00:00\", \"total_distance\": 2.00, \"duration\": 720.0, \"pace\": 6.0, \"calories\": 140.0, \"segment_paces\": [5.8, 6.2],\n" +
                    "    \"gps_traces\": [\n" +
                    "      {\"latitude\": 37.6180, \"longitude\": 126.7120, \"time\": \"2026-05-03T07:00:00\"}, {\"latitude\": 37.6185, \"longitude\": 126.7135, \"time\": \"2026-05-03T07:01:20\"},\n" +
                    "      {\"latitude\": 37.6190, \"longitude\": 126.7150, \"time\": \"2026-05-03T07:02:40\"}, {\"latitude\": 37.6200, \"longitude\": 126.7165, \"time\": \"2026-05-03T07:04:10\"},\n" +
                    "      {\"latitude\": 37.6210, \"longitude\": 126.7180, \"time\": \"2026-05-03T07:05:50\"}, {\"latitude\": 37.6225, \"longitude\": 126.7190, \"time\": \"2026-05-03T07:07:30\"},\n" +
                    "      {\"latitude\": 37.6240, \"longitude\": 126.7185, \"time\": \"2026-05-03T07:09:10\"}, {\"latitude\": 37.6235, \"longitude\": 126.7170, \"time\": \"2026-05-03T07:10:40\"},\n" +
                    "      {\"latitude\": 37.6215, \"longitude\": 126.7155, \"time\": \"2026-05-03T07:12:00\"}\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"plan_id\": null,\n" + // 수동으로 오늘 플랜 ID와 매칭
                    "    \"created_at\": \"2026-05-03T11:00:00\", \"total_distance\": 3.50, \"duration\": 1302.0, \"pace\": 6.2, \"calories\": 245.0, \"segment_paces\": [4.2, 5.5, 7.2],\n" +
                    "    \"gps_traces\": [\n" +
                    "      {\"latitude\": 37.6150, \"longitude\": 126.7100, \"time\": \"2026-05-03T11:00:00\"}, {\"latitude\": 37.6160, \"longitude\": 126.7115, \"time\": \"2026-05-03T11:01:10\"},\n" +
                    "      {\"latitude\": 37.6175, \"longitude\": 126.7130, \"time\": \"2026-05-03T11:02:30\"}, {\"latitude\": 37.6190, \"longitude\": 126.7150, \"time\": \"2026-05-03T11:04:00\"},\n" +
                    "      {\"latitude\": 37.6210, \"longitude\": 126.7170, \"time\": \"2026-05-03T11:05:40\"}, {\"latitude\": 37.6230, \"longitude\": 126.7190, \"time\": \"2026-05-03T11:07:30\"},\n" +
                    "      {\"latitude\": 37.6250, \"longitude\": 126.7205, \"time\": \"2026-05-03T11:09:20\"}, {\"latitude\": 37.6265, \"longitude\": 126.7195, \"time\": \"2026-05-03T11:11:30\"},\n" +
                    "      {\"latitude\": 37.6270, \"longitude\": 126.7175, \"time\": \"2026-05-03T11:13:50\"}, {\"latitude\": 37.6260, \"longitude\": 126.7155, \"time\": \"2026-05-03T11:16:10\"},\n" +
                    "      {\"latitude\": 37.6245, \"longitude\": 126.7140, \"time\": \"2026-05-03T11:18:40\"}, {\"latitude\": 37.6225, \"longitude\": 126.7125, \"time\": \"2026-05-03T11:21:20\"}\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"plan_id\": 1,\n" +
                    "    \"created_at\": \"2026-05-02T09:00:00\", \"total_distance\": 3.20, \"duration\": 1020.0, \"pace\": 5.3, \"calories\": 280.0, \"segment_paces\": [4.5, 5.2, 6.8],\n" +
                    "    \"gps_traces\": [\n" +
                    "      {\"latitude\": 37.6220, \"longitude\": 126.7110, \"time\": \"2026-05-02T09:00:00\"}, {\"latitude\": 37.6235, \"longitude\": 126.7125, \"time\": \"2026-05-02T09:01:15\"},\n" +
                    "      {\"latitude\": 37.6250, \"longitude\": 126.7145, \"time\": \"2026-05-02T09:02:40\"}, {\"latitude\": 37.6240, \"longitude\": 126.7160, \"time\": \"2026-05-02T09:04:10\"},\n" +
                    "      {\"latitude\": 37.6220, \"longitude\": 126.7180, \"time\": \"2026-05-02T09:06:00\"}, {\"latitude\": 37.6200, \"longitude\": 126.7195, \"time\": \"2026-05-02T09:08:10\"},\n" +
                    "      {\"latitude\": 37.6185, \"longitude\": 126.7175, \"time\": \"2026-05-02T09:10:30\"}, {\"latitude\": 37.6175, \"longitude\": 126.7155, \"time\": \"2026-05-02T09:13:00\"},\n" +
                    "      {\"latitude\": 37.6185, \"longitude\": 126.7135, \"time\": \"2026-05-02T09:15:20\"}, {\"latitude\": 37.6200, \"longitude\": 126.7120, \"time\": \"2026-05-02T09:17:00\"}\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"plan_id\": null,\n" +
                    "    \"created_at\": \"2026-05-01T10:00:00\", \"total_distance\": 3.00, \"duration\": 1170.0, \"pace\": 6.5, \"calories\": 210.0, \"segment_paces\": [6.2, 6.4, 6.8, 6.5, 6.6],\n" +
                    "    \"gps_traces\": [\n" +
                    "      {\"latitude\": 37.6180, \"longitude\": 126.7120, \"time\": \"2026-05-01T10:00:00\"}, {\"latitude\": 37.6185, \"longitude\": 126.7130, \"time\": \"2026-05-01T10:01:00\"},\n" +
                    "      {\"latitude\": 37.6192, \"longitude\": 126.7145, \"time\": \"2026-05-01T10:02:00\"}, {\"latitude\": 37.6200, \"longitude\": 126.7160, \"time\": \"2026-05-01T10:03:00\"},\n" +
                    "      {\"latitude\": 37.6210, \"longitude\": 126.7175, \"time\": \"2026-05-01T10:04:30\"}, {\"latitude\": 37.6220, \"longitude\": 126.7185, \"time\": \"2026-05-01T10:06:00\"},\n" +
                    "      {\"latitude\": 37.6235, \"longitude\": 126.7190, \"time\": \"2026-05-01T10:07:30\"}, {\"latitude\": 37.6245, \"longitude\": 126.7185, \"time\": \"2026-05-01T10:09:00\"},\n" +
                    "      {\"latitude\": 37.6255, \"longitude\": 126.7175, \"time\": \"2026-05-01T10:10:30\"}, {\"latitude\": 37.6260, \"longitude\": 126.7160, \"time\": \"2026-05-01T10:12:00\"},\n" +
                    "      {\"latitude\": 37.6255, \"longitude\": 126.7145, \"time\": \"2026-05-01T10:13:30\"}, {\"latitude\": 37.6245, \"longitude\": 126.7130, \"time\": \"2026-05-01T10:15:00\"},\n" +
                    "      {\"latitude\": 37.6230, \"longitude\": 126.7120, \"time\": \"2026-05-01T10:16:30\"}, {\"latitude\": 37.6215, \"longitude\": 126.7115, \"time\": \"2026-05-01T10:17:30\"},\n" +
                    "      {\"latitude\": 37.6200, \"longitude\": 126.7110, \"time\": \"2026-05-01T10:18:30\"}, {\"latitude\": 37.6185, \"longitude\": 126.7115, \"time\": \"2026-05-01T10:19:30\"}\n" +
                    "    ]\n" +
                    "  }\n" +
                    "]";
            return buildResponse(chain, json);
        }

        // 2. [코칭 목표] plan_day_id -> plan_id로 수정
        if (uri.contains("coaching/goals/active")) {
            String json = "{\n" +
                    "  \"goal_id\": 123,\n" +
                    "  \"has_active_goal\": true,\n" +
                    "  \"status\": \"active\",\n" +
                    "  \"goal\": {\n" +
                    "    \"goal_id\": 123, \"period_type\": \"custom\", \"custom_weeks\": 3, \"running_days\": [\"sun\", \"mon\", \"tue\", \"wed\", \"sat\"], \"goal_distance_km\": 10.0, \"goal_pace_min_per_km\": 5.5, \"start_date\": \"2026-05-01\", \"end_date\": \"2026-05-21\"\n" +
                    "  },\n" +
                    "  \"plan_days\": [\n" +
                    "    {\"plan_id\": 1, \"plan_date\": \"2026-05-02\", \"day_distance_km\": 3.2, \"day_pace_min_per_km\": 6.0, \"description\": \"첫 코칭을 완료하셨네요! 대단해요! 해당 데이터를 가지고 앞으로의 계획을 조정합니다!\", \"is_completed\": true},\n" +
                    "    {\"plan_id\": 2, \"plan_date\": \"2026-05-03\", \"day_distance_km\": 3.5, \"day_pace_min_per_km\": 6.2, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 3, \"plan_date\": \"2026-05-04\", \"day_distance_km\": 3.5, \"day_pace_min_per_km\": 6.2, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 4, \"plan_date\": \"2026-05-05\", \"day_distance_km\": 3.8, \"day_pace_min_per_km\": 6.1, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 5, \"plan_date\": \"2026-05-06\", \"day_distance_km\": 4.0, \"day_pace_min_per_km\": 6.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 6, \"plan_date\": \"2026-05-09\", \"day_distance_km\": 4.0, \"day_pace_min_per_km\": 6.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 7, \"plan_date\": \"2026-05-10\", \"day_distance_km\": 4.5, \"day_pace_min_per_km\": 5.8, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 8, \"plan_date\": \"2026-05-11\", \"day_distance_km\": 4.5, \"day_pace_min_per_km\": 5.8, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 9, \"plan_date\": \"2026-05-12\", \"day_distance_km\": 4.8, \"day_pace_min_per_km\": 5.7, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 10, \"plan_date\": \"2026-05-13\", \"day_distance_km\": 5.0, \"day_pace_min_per_km\": 5.5, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 11, \"plan_date\": \"2026-05-16\", \"day_distance_km\": 5.0, \"day_pace_min_per_km\": 5.5, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 12, \"plan_date\": \"2026-05-17\", \"day_distance_km\": 5.5, \"day_pace_min_per_km\": 5.2, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 13, \"plan_date\": \"2026-05-18\", \"day_distance_km\": 5.5, \"day_pace_min_per_km\": 5.2, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 14, \"plan_date\": \"2026-05-19\", \"day_distance_km\": 5.8, \"day_pace_min_per_km\": 5.1, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false},\n" +
                    "    {\"plan_id\": 15, \"plan_date\": \"2026-05-20\", \"day_distance_km\": 6.0, \"day_pace_min_per_km\": 5.0, \"description\": \"당일의 목표를 완수하시면 AI가 피드백을 남깁니다.\", \"is_completed\": false}\n" +
                    "  ]\n" +
                    "}";
            return buildResponse(chain, json);
        }

        // 3. [오늘 플랜] plan_day_id -> plan_id로 수정
        if (uri.contains("coaching/plans/today")) {
            String json = "{\"has_plan\": true, \"plan_day\": {\"plan_id\": 2, \"plan_date\": \"2026-05-03\", \"day_distance_km\": 3.5, \"day_pace_min_per_km\": 6.2, \"is_completed\": false}, \"goal\": {\"goal_distance_km\": 10.0, \"goal_pace_min_per_km\": 5.5}}";
            return buildResponse(chain, json);
        }

        if (method.equals("POST") && uri.contains("feedback")) {
            return buildResponse(chain, "{\"plan_id\": 2, \"is_completed\": true, \"ai_feedback_comment\": \"정확한 세팅 확인 완료!\"}");
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
                        "  \"profile_photo\": null,\n" +
                        "  \"status_message\": \"항상 열심히 운동하자!\",\n" +
                        "  \"friend_count\": 15,\n" +
                        "  \"post_count\": 12,\n" +
                        "  \"tagged_count\": 5,\n" + // 쉼표 주의!
                        "  \"commented_feed_count\": 8,\n" +
                        "  \"liked_feed_count\": 0,\n" +
                        "  \"bookmarked_feed_count\": 3\n" +
                        "}";
                return buildResponse(chain, json);
            }
        }

        // 5. [마이페이지 피드 목록 통합 관리]
        if (uri.contains("community/contents")) {
            String json = ""; // 결과를 담을 변수 선언

            if (uri.endsWith("/me")) {
                // 내가 쓴 피드
                json = "[\n" +
                        "  {\n" +
                        "    \"content_id\": 101,\n" +
                        "    \"content_text\": \"오운완! 근육 식물 아바타 귀엽죠?\",\n" +
                        "    \"total_distance\": 11.8,\n" +
                        "    \"duration\": 2269,\n" + // 37분 49초
                        "    \"pace\": 384,\n" + // 6:24
                        "    \"created_at\": \"2026-05-05T17:10:00\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"content_id\": 102,\n" +
                        "    \"content_text\": \"유산소 빡세게 한 날... 힘들다.\",\n" +
                        "    \"total_distance\": 22.3,\n" +
                        "    \"duration\": 9158,\n" +
                        "    \"pace\": 410,\n" +
                        "    \"created_at\": \"2026-03-02T09:00:00\"\n" +
                        "  }\n" +
                        "]";
            }
            else if (uri.endsWith("/tagged")) {
                // 나를 태그한 피드
                json = "[{\"content_id\": 201, \"content_text\": \"@MocktestID1557 님과 함께 달렸어요!\", \"total_distance\": 5.5, \"duration\": 1200, \"pace\": 360, \"created_at\": \"2026-05-01T10:00:00\"}]";
            }
            else if (uri.endsWith("/comments")) {
                // 내가 댓글 단 피드
                json = "[{\"content_id\": 301, \"content_text\": \"댓글 단 피드 예시입니다.\", \"total_distance\": 8.2, \"duration\": 2400, \"pace\": 400, \"created_at\": \"2026-04-20T14:00:00\"}]";
            }
            else if (uri.endsWith("/likes")) {
                // 내가 좋아요 한 피드
                json = "[{\"content_id\": 401, \"content_text\": \"좋아요를 누른 멋진 피드!\", \"total_distance\": 10.0, \"duration\": 3000, \"pace\": 300, \"created_at\": \"2026-04-15T09:00:00\"}]";
            }
            else if (uri.endsWith("/bookmarks")) {
                // 내가 북마크 한 피드
                json = "[{\"content_id\": 501, \"content_text\": \"나중에 다시 볼 북마크 피드\", \"total_distance\": 15.3, \"duration\": 4500, \"pace\": 350, \"created_at\": \"2026-04-10T20:00:00\"}]";
            }

            return buildResponse(chain, json);
        }


        // Mockserver 안에서 프로필 수정(PUT) 응답
        if (method.equals("DELETE")) return buildResponse(chain, "{\"status\": \"success\"}");


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


        return chain.proceed(chain.request());
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
