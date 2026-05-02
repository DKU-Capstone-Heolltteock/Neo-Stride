package com.neostride.app.feature.coaching.api;

import com.neostride.app.feature.coaching.model.FeedbackRequest;
import com.neostride.app.feature.coaching.model.FeedbackResponse;
import com.neostride.app.feature.coaching.model.GoalRequest;
import com.neostride.app.feature.coaching.model.GoalResponse;
import com.neostride.app.feature.coaching.model.TodayPlanResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Map;

public interface CoachingApi {

    // 목표 생성 (AI 플랜 생성 포함)
    @POST("/api/coaching/goals")
    Call<GoalResponse> createGoal(@Body GoalRequest request);

    // 현재 활성 목표 + 플랜 목록 조회
    @GET("/api/coaching/goals/active")
    Call<GoalResponse> getActiveGoal(@Query("user_id") int userId);

    // 오늘 플랜 조회 (러닝 탭 코칭 버튼용)
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
}