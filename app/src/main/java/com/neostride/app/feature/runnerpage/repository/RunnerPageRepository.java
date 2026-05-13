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


//  러너 페이지 데이터 레포지터리
//  <p>
//  - 프로필·배지·피드를 서버에서 가져와 콜백으로 전달한다.

public class RunnerPageRepository {

    private final RunnerPageService apiService;

    public RunnerPageRepository() {
        this.apiService = ApiClient.getInstance().create(RunnerPageService.class);
    }

    // 러너 프로필을 조회해 Retrofit Callback으로 반환
    public void getRunnerProfile(int userId, Callback<RunnerProfileResponse> callback) {
        apiService.getRunnerProfile(userId).enqueue(callback);
    }

    // 러너 배지를 조회해 {@link RunnerBadgeCallback}으로 반환
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

    // 러너 피드 목록을 조회해 Retrofit Callback으로 반환
    public void getRunnerFeeds(int userId, Callback<List<CommunityContentResponse>> callback) {
        apiService.getRunnerFeeds(userId).enqueue(callback);
    }

    // 배지 조회 성공 콜백 인터페이스
    public interface RunnerBadgeCallback {
        void onSuccess(BadgeDetailResponse response);
    }
}
