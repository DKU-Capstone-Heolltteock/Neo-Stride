package com.neostride.app.feature.friend.model;

import com.google.gson.annotations.SerializedName;

public class FriendResponse {
    @SerializedName("user_id")
    public int userId;

    @SerializedName("nickname")
    public String nickname;

    @SerializedName("badge_tier") // 유저가 획득한 티어 (ex: challenger)
    public String badgeTier;

    @SerializedName("friend_count")
    public int friendCount;

    @SerializedName("profile_image_url")
    public String profileImageUrl;

    @SerializedName("status") // 관계 상태 (friend, sent, received, blocked)
    public String status;
}