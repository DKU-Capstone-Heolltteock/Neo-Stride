package com.neostride.app.feature.badge.api;

import com.neostride.app.feature.badge.model.BadgeDetailResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface BadgeService {
    @GET("users/me/badge") // Mockserver에서 설정한 경로와 일치해야 함
    Call<BadgeDetailResponse> getBadgeDetail();
}
