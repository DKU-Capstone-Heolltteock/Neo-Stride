package com.neostride.app.feature.main.coaching.api;

import com.neostride.app.feature.main.coaching.model.FeedbackRequest;
import com.neostride.app.feature.main.coaching.model.FeedbackResponse;
import com.neostride.app.feature.main.coaching.model.GoalRequest;
import com.neostride.app.feature.main.coaching.model.GoalResponse;
import com.neostride.app.feature.main.coaching.model.TodayPlanResponse;
import com.neostride.app.feature.main.coaching.model.GoalStatusUpdateRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.PATCH; // 서버의 DB 값을 바꿀 수 있도록(goal 테이블 is_achived 항목

import java.util.Map;


//  코칭 API 인터페이스
//  <p>
//  - 목표 생성/조회/삭제, 오늘 플랜 조회, AI 피드백 요청, 목표 상태 변경 엔드포인트를 정의한다.

public interface CoachingApi {

    // 목표 생성 (서버에서 AI 플랜도 함께 생성)
    @POST("/api/coaching/goals")
    Call<GoalResponse> createGoal(@Body GoalRequest request);

    // 현재 활성 목표 + 날짜별 플랜 목록 조회
    @GET("/api/coaching/goals/active")
    Call<GoalResponse> getActiveGoal(@Query("user_id") int userId);

    // 오늘의 플랜 조회 (러닝 탭 코칭 버튼 표시 여부 판단용)
    @GET("/api/coaching/plans/today")
    Call<TodayPlanResponse> getTodayPlan(@Query("user_id") int userId);

    // 러닝 완료 후 AI 피드백 요청
    @POST("/api/coaching/plans/{plan_day_id}/feedback")
    Call<FeedbackResponse> requestFeedback(
            @Path("plan_day_id") int planDayId,
            @Body FeedbackRequest request
    );

    // 목표 삭제
    @DELETE("/api/coaching/goals/{goal_id}")
    Call<Map<String, String>> deleteGoal(@Path("goal_id") int goalId);

    // 목표 달성 여부에 따라 DB 상태 변경 (조기 졸업 및 기간 만료 처리)
    @PATCH("/api/coaching/goals/{goal_id}/status")
    Call<GoalResponse> updateGoalStatus(
            @Path("goal_id") int goalId,
            @Body GoalStatusUpdateRequest request
    );
}