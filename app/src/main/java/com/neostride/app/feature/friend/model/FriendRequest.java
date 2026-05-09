package com.neostride.app.feature.friend.model;

import com.google.gson.annotations.SerializedName;

public class FriendRequest {
    @SerializedName("target_id")
    private int targetId;

    @SerializedName("action") // "cancel", "accept", "reject", "block"
    private String action;

    public FriendRequest(int targetId, String action) {
        this.targetId = targetId;
        this.action = action;
    }
}