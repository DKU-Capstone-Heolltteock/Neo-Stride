package com.neostride.app.feature.feed.model;

import com.google.gson.annotations.SerializedName;

/*
 * 태그할 수 있는 사용자 정보를 저장하는 DTO 클래스임
 * 친구 목록 API 응답을 받아서 사람 태그 다이얼로그에 표시할 때 사용함
 */
public class TagUser {

    // 사용자 고유 ID임
    // 서버 JSON에서는 user_id로 내려옴
    @SerializedName("user_id")
    private Long userId;

    // 사용자 닉네임임
    private String nickname;

    // 사용자 뱃지 등급임
    // 서버 JSON에서는 badge_tier로 내려옴
    @SerializedName("badge_tier")
    private String badgeTier;

    // 친구 수임
    // 서버 JSON에서는 friend_count로 내려옴
    @SerializedName("friend_count")
    private int friendCount;

    // 프로필 이미지 URL임
    // 서버 JSON에서는 profile_image_url로 내려옴
    @SerializedName("profile_image_url")
    private String profileImageUrl;

    // 친구 상태값임
    // 예: ACCEPTED, REQUESTED 등
    private String status;

    /*
     * Mock 데이터나 직접 객체 생성 시 사용할 생성자임
     */
    public TagUser(Long userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;
    }

    /*
     * 전체 필드를 직접 넣어야 할 때 사용할 생성자임
     */
    public TagUser(
            Long userId,
            String nickname,
            String badgeTier,
            int friendCount,
            String profileImageUrl,
            String status
    ) {
        this.userId = userId;
        this.nickname = nickname;
        this.badgeTier = badgeTier;
        this.friendCount = friendCount;
        this.profileImageUrl = profileImageUrl;
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getBadgeTier() {
        return badgeTier;
    }

    public int getFriendCount() {
        return friendCount;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getStatus() {
        return status;
    }
}