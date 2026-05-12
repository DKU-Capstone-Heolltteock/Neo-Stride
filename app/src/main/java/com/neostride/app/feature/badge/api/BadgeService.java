package com.neostride.app.feature.badge.api;

import com.neostride.app.feature.badge.model.BadgeDetailResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface BadgeService {
    @GET("users/me/badge")
    Call<BadgeDetailResponse> getBadgeDetail();

    @GET("users/{userId}/badge")
    Call<BadgeDetailResponse> getBadgeDetailByUserId(@Path("userId") int userId);
}
