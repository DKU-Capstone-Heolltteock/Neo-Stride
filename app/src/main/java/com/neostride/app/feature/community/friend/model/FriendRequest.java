package com.neostride.app.feature.community.friend.model;

import com.google.gson.annotations.SerializedName;


//  친구 관계 변경 요청 DTO
//  <p>
//  POST community/friends/action — action 값으로 관계를 변경한다.
//  (action: "cancel" | "accept" | "reject" | "block" | "delete" | "request")

public class FriendRequest {
    @SerializedName("target_id")
    private int targetId;

    @SerializedName("action")
    private String action;

    public FriendRequest(int targetId, String action) {
        this.targetId = targetId;
        this.action = action;
    }
}