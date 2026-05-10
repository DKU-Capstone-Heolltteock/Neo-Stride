package com.neostride.app.feature.friend.model;

import com.google.gson.annotations.SerializedName;

// FriendResponse.java
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