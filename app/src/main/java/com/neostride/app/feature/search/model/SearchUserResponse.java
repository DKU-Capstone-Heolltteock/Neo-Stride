package com.neostride.app.feature.search.model;

import com.google.gson.annotations.SerializedName;

/*
 * 프로필/친구 검색 결과 DTO 클래스임
 * 피드/팁은 기존 FeedResponse, TipResponse를 쓰고
 * 사용자 검색 결과만 별도 DTO로 받음
 */
public class SearchUserResponse {

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("nickname")
    private String nickname;

    @SerializedName("profile_image_url")
    private String profileImageUrl;

    @SerializedName("status_message")
    private String statusMessage;

    @SerializedName("friend_count")
    private int friendCount;

    /*
     * 배지 등급임 (bronze/silver/gold/platinum/diamond/master/challenger/none)
     */
    @SerializedName("badge_tier")
    private String badgeTier;

    /*
     * 나와의 친구 관계 상태임
     * none / sent / received / friends
     */
    @SerializedName("status")
    private String status;

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getFriendCount() {
        return friendCount;
    }

    public String getBadgeTier() {
        return badgeTier;
    }

    public String getStatus() {
        return status;
    }

    public boolean isFriend() {
        return "friends".equals(status);
    }

    /*
     * 친구 액션 완료 후 로컬 상태를 갱신할 때 사용함
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
