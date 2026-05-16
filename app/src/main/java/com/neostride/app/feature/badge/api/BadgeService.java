package com.neostride.app.feature.badge.api;

import com.neostride.app.feature.badge.model.BadgeDetailResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;


//  뱃지 API 인터페이스
//  <p>
//  - 내 뱃지 조회 및 특정 사용자 뱃지 조회 엔드포인트를 정의한다.

public interface BadgeService {
    // 현재 로그인 사용자의 뱃지 상세 조회
    @GET("users/me/badge")
    Call<BadgeDetailResponse> getBadgeDetail();

    // 지정 사용자의 뱃지 상세 조회
    @GET("users/{userId}/badge")
    Call<BadgeDetailResponse> getBadgeDetailByUserId(@Path("userId") int userId);
}
