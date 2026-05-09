package com.neostride.app.feature.badge.model;

import com.google.gson.annotations.SerializedName;

public class BadgeDetailResponse {
    @SerializedName("Badge")
    public String tier; // none, bronze, silver, gold, platinum...

    @SerializedName("record_id")
    public int recordId;

    @SerializedName("distance")
    public double distance;

    @SerializedName("pace")
    public String pace;

    @SerializedName("achieved_at")
    public String achievedAt;
}
