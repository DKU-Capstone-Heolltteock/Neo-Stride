package com.neostride.app.feature.runnerpage.repository;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.badge.model.BadgeDetailResponse;
import com.neostride.app.feature.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.runnerpage.api.RunnerPageService;
import com.neostride.app.feature.runnerpage.model.RunnerProfileResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RunnerPageRepository {

    private final RunnerPageService apiService;

    public RunnerPageRepository() {
        this.apiService = ApiClient.getInstance().create(RunnerPageService.class);
    }

    // 러너 프로필 조회
    public void getRunnerProfile(int userId, Callback<RunnerProfileResponse> callback) {
        apiService.getRunnerProfile(userId).enqueue(callback);
    }

    // 러너 배지 조회
    public void getRunnerBadge(int userId, RunnerBadgeCallback callback) {
        apiService.getRunnerBadge(userId).enqueue(new Callback<BadgeDetailResponse>() {
            @Override
            public void onResponse(Call<BadgeDetailResponse> call, Response<BadgeDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                }
            }
            @Override
            public void onFailure(Call<BadgeDetailResponse> call, Throwable t) {}
        });
    }

    // 러너 피드 목록 조회
    public void getRunnerFeeds(int userId, Callback<List<CommunityContentResponse>> callback) {
        apiService.getRunnerFeeds(userId).enqueue(callback);
    }

    public interface RunnerBadgeCallback {
        void onSuccess(BadgeDetailResponse response);
    }
}
