package com.neostride.app.feature.runnerpage.model;

import com.google.gson.annotations.SerializedName;

//  러너 프로필 API 응답 DTO
//  <p>
//  - 닉네임·프로필 사진·상태메시지·친구 수·게시물 수·친구 여부를 담는다.

public class RunnerProfileResponse {
    @SerializedName("community_profile_name") public String nickname;
    @SerializedName("profile_photo")          public String profilePhoto;
    @SerializedName("status_message")         public String statusMessage;
    @SerializedName("friend_count")           public Integer friendCount;
    @SerializedName("post_count")             public Integer postCount;
    /** 백엔드에서 내려주는 로그인 유저와의 친구 여부 (null이면 false로 처리) */
    @SerializedName("is_friend")              public Boolean isFriend;
}
