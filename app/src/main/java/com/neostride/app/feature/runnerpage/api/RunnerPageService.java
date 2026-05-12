package com.neostride.app.feature.runnerpage.api;

import com.neostride.app.feature.badge.model.BadgeDetailResponse;
import com.neostride.app.feature.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.runnerpage.model.RunnerProfileResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RunnerPageService {

    @GET("users/{userId}/profile")
    Call<RunnerProfileResponse> getRunnerProfile(@Path("userId") int userId);

    @GET("users/{userId}/badge")
    Call<BadgeDetailResponse> getRunnerBadge(@Path("userId") int userId);

    @GET("community/contents/user/{userId}")
    Call<List<CommunityContentResponse>> getRunnerFeeds(@Path("userId") int userId);
}
