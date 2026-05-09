package com.neostride.app.feature.badge.repository;

import com.neostride.app.feature.badge.api.BadgeService;
import com.neostride.app.feature.badge.model.BadgeDetailResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BadgeRepository {
    private final BadgeService badgeService;

    public BadgeRepository(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    public void fetchBadgeDetail(BadgeCallback callback) {
        badgeService.getBadgeDetail().enqueue(new Callback<BadgeDetailResponse>() {
            @Override
            public void onResponse(Call<BadgeDetailResponse> call, Response<BadgeDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(Call<BadgeDetailResponse> call, Throwable t) {
                // 실패 시 로직
            }
        });
    }

    public interface BadgeCallback {
        void onSuccess(BadgeDetailResponse response);
    }
}