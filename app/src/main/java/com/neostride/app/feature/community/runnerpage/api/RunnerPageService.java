package com.neostride.app.feature.community.runnerpage.api;

import com.neostride.app.feature.badge.model.BadgeDetailResponse;
import com.neostride.app.feature.community.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.community.runnerpage.model.RunnerProfileResponse;
import com.neostride.app.feature.community.tip.model.TipResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;


//  러너 페이지 API 인터페이스
//  <p>
//  - 특정 사용자의 프로필·배지·피드를 조회한다.
public interface RunnerPageService {

    // 사용자 프로필(닉네임·상태메시지·친구 수 등) 조회
    @GET("users/{userId}/profile")
    Call<RunnerProfileResponse> getRunnerProfile(@Path("userId") int userId);

    // 사용자 배지 정보 조회
    @GET("users/{userId}/badge")
    Call<BadgeDetailResponse> getRunnerBadge(@Path("userId") int userId);

    // 사용자 작성 피드 목록 조회
    @GET("community/contents/user/{userId}")
    Call<List<CommunityContentResponse>> getRunnerFeeds(@Path("userId") int userId);

    // 사용자 작성 팁 목록 조회
    @GET("community/tips/user/{userId}")
    Call<List<TipResponse>> getRunnerTips(@Path("userId") int userId);
}
