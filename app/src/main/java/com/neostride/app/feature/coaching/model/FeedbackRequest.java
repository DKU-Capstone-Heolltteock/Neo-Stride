package com.neostride.app.feature.coaching.model;

import com.google.gson.annotations.SerializedName;


//  러닝 완료 후 AI 피드백 요청 DTO
//  POST /api/coaching/plans/{plan_day_id}/feedback

public class FeedbackRequest {

    @SerializedName("plan_day_id")
    private int planDayId;

    @SerializedName("actual_distance_km")
    private float actualDistanceKm;

    @SerializedName("actual_time_sec")
    private int actualTimeSec;

    @SerializedName("actual_pace_min_per_km")
    private float actualPaceMinPerKm;

    public FeedbackRequest(int planDayId, float actualDistanceKm,
                           int actualTimeSec, float actualPaceMinPerKm) {
        this.planDayId = planDayId;
        this.actualDistanceKm = actualDistanceKm;
        this.actualTimeSec = actualTimeSec;
        this.actualPaceMinPerKm = actualPaceMinPerKm;
    }
}