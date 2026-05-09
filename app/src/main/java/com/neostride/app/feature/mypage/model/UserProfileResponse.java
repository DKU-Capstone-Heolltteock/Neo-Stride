package com.neostride.app.feature.mypage.model;

import com.google.gson.annotations.SerializedName;

public class UserProfileResponse {
    @SerializedName("community_profile_name") public String nickname;
    @SerializedName("profile_photo") public String profilePhoto;
    @SerializedName("status_message") public String statusMessage;
    @SerializedName("friend_count") public Integer friendCount;
    @SerializedName("post_count") public Integer postCount;
    @SerializedName("tagged_count") public Integer taggedCount;
    @SerializedName("commented_feed_count") public Integer commentedFeedCount;
    @SerializedName("liked_feed_count") public Integer likedFeedCount;
    @SerializedName("bookmarked_feed_count") public Integer bookmarkedFeedCount;

}
