package com.neostride.app.feature.main.coaching.repository;

import android.util.Log;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.main.coaching.api.CoachingApi;
import com.neostride.app.feature.main.coaching.model.FeedbackRequest;
import com.neostride.app.feature.main.coaching.model.FeedbackResponse;
import com.neostride.app.feature.main.coaching.model.GoalRequest;
import com.neostride.app.feature.main.coaching.model.GoalResponse;
import com.neostride.app.feature.main.coaching.model.GoalStatusUpdateRequest;
import com.neostride.app.feature.main.coaching.model.TodayPlanResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Map;


//  코칭 데이터 레포지터리
//  <p>
//  - {@link CoachingApi}를 통해 목표·플랜·피드백 API를 호출하고 결과를 {@link OnResultListener}로 전달한다.

public class CoachingRepository {

    private static final String TAG = "CoachingRepository";
    private final CoachingApi coachingApi;

    // 코칭 API 결과 콜백 인터페이스
    public interface OnResultListener<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public CoachingRepository() {
        this.coachingApi = ApiClient.getInstance().create(CoachingApi.class);
    }

    // ── 목표 생성 (AI 플랜 생성 포함) ──
    public void createGoal(GoalRequest request, OnResultListener<GoalResponse> listener) {
        coachingApi.createGoal(request).enqueue(new Callback<GoalResponse>() {
            @Override
            public void onResponse(Call<GoalResponse> call, Response<GoalResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "목표 생성 성공! goal_id=" + response.body().getGoalId());
                    listener.onSuccess(response.body());
                } else {
                    listener.onError("목표 생성 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GoalResponse> call, Throwable t) {
                Log.e(TAG, "목표 생성 네트워크 오류", t);
                listener.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // ── 현재 활성 목표 조회 ──
    public void getActiveGoal(int userId, OnResultListener<GoalResponse> listener) {
        coachingApi.getActiveGoal(userId).enqueue(new Callback<GoalResponse>() {
            @Override
            public void onResponse(Call<GoalResponse> call, Response<GoalResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onSuccess(response.body());
                } else {
                    listener.onError("목표 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GoalResponse> call, Throwable t) {
                listener.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // ── 오늘 플랜 조회 ──
    public void getTodayPlan(int userId, OnResultListener<TodayPlanResponse> listener) {
        coachingApi.getTodayPlan(userId).enqueue(new Callback<TodayPlanResponse>() {
            @Override
            public void onResponse(Call<TodayPlanResponse> call, Response<TodayPlanResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onSuccess(response.body());
                } else {
                    listener.onError("오늘 플랜 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TodayPlanResponse> call, Throwable t) {
                listener.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // ── AI 피드백 요청 ──
    public void requestFeedback(int planDayId, FeedbackRequest request, OnResultListener<FeedbackResponse> listener) {
        coachingApi.requestFeedback(planDayId, request).enqueue(new Callback<FeedbackResponse>() {
            @Override
            public void onResponse(Call<FeedbackResponse> call, Response<FeedbackResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "AI 피드백 수신 완료");
                    listener.onSuccess(response.body());
                } else {
                    listener.onError("피드백 요청 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<FeedbackResponse> call, Throwable t) {
                listener.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // ── 목표 상태 변경 (is_active, is_achieved) ──
    public void updateGoalStatus(int goalId, GoalStatusUpdateRequest request, OnResultListener<GoalResponse> listener) {
        coachingApi.updateGoalStatus(goalId, request).enqueue(new Callback<GoalResponse>() {
            @Override
            public void onResponse(Call<GoalResponse> call, Response<GoalResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "목표 상태 변경 성공 goal_id=" + goalId);
                    listener.onSuccess(response.body());
                } else {
                    listener.onError("상태 변경 실패: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<GoalResponse> call, Throwable t) {
                Log.e(TAG, "목표 상태 변경 네트워크 오류", t);
                listener.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // ── 목표 삭제 ──
    public void deleteGoal(int goalId, OnResultListener<String> listener) {
        coachingApi.deleteGoal(goalId).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "목표 삭제 성공");
                    listener.onSuccess("삭제 완료");
                } else {
                    listener.onError("삭제 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                listener.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }
}
