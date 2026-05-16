package com.neostride.app.feature.friend.model;

import com.google.gson.annotations.SerializedName;


//  친구 목록 항목 응답 DTO
//  <p>
//  - 사용자 기본 정보(닉네임, 배지 등급, 친구 수)와 관계 상태를 담는다.

public class FriendResponse {
    @SerializedName("user_id")
    public int userId;

    @SerializedName("nickname")
    public String nickname;

    @SerializedName("badge_tier")
    public String badgeTier;

    @SerializedName("friend_count")
    public int friendCount;

    @SerializedName("profile_image_url")
    public String profileImageUrl;

    @SerializedName("status")
    public String status;
}