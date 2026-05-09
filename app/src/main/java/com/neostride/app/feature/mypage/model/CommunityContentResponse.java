package com.neostride.app.feature.mypage.model;

import com.google.gson.annotations.SerializedName;

public class CommunityContentResponse {
    @SerializedName("content_id") public int contentId;
    @SerializedName("content_text") public String contentText;
    @SerializedName("total_distance") public double totalDistance;
    @SerializedName("duration") public int duration;
    @SerializedName("pace") public int pace;
    @SerializedName("created_at") public String createdAt;
}