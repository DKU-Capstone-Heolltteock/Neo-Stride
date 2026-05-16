package com.neostride.app.feature.badge.repository;

import com.neostride.app.feature.badge.api.BadgeService;
import com.neostride.app.feature.badge.model.BadgeDetailResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 뱃지 데이터 레포지터리
 * <p>
 * - {@link BadgeService}를 통해 뱃지 API를 호출하고 결과를 {@link BadgeCallback}으로 전달한다.
 */
public class BadgeRepository {
    private final BadgeService badgeService;

    public BadgeRepository(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    /** 지정 사용자의 뱃지 상세 정보를 서버에서 조회한다. */
    public void fetchBadgeDetailByUserId(int userId, BadgeCallback callback) {
        badgeService.getBadgeDetailByUserId(userId).enqueue(new Callback<BadgeDetailResponse>() {
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

    /** 현재 로그인 사용자의 뱃지 상세 정보를 서버에서 조회한다. */
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