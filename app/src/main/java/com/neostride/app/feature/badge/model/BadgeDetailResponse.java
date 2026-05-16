package com.neostride.app.feature.badge.model;

import com.google.gson.annotations.SerializedName;


//  뱃지 상세 정보 응답 DTO
//  <p>
//  - 서버에서 사용자 뱃지 등급, 달성 기록, 달성일자를 받아온다.

public class BadgeDetailResponse {
    // 등급 문자열 (예: "none", "bronze", "gold")
    @SerializedName("Badge")
    public String tier;

    @SerializedName("record_id")
    public int recordId;

    @SerializedName("distance")
    public double distance;

    @SerializedName("pace")
    public String pace;

    @SerializedName("achieved_at")
    public String achievedAt;
}
