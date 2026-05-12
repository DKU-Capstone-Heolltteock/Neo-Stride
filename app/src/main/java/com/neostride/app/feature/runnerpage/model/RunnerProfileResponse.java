package com.neostride.app.feature.runnerpage.model;

import com.google.gson.annotations.SerializedName;

public class RunnerProfileResponse {
    @SerializedName("community_profile_name") public String nickname;
    @SerializedName("profile_photo")          public String profilePhoto;
    @SerializedName("status_message")         public String statusMessage;
    @SerializedName("friend_count")           public Integer friendCount;
    @SerializedName("post_count")             public Integer postCount;
    /** 백엔드에서 내려주는 로그인 유저와의 친구 여부 (null이면 false로 처리) */
    @SerializedName("is_friend")              public Boolean isFriend;
}
