package com.neostride.app.feature.coaching.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;


//  목표 생성 요청 DTO
//  ERD: COACHING_PLANS 테이블에 저장될 데이터
//  POST /api/coaching/goals

public class GoalRequest {

    @SerializedName("user_id")
    private int userId;

    @SerializedName("period_type")
    private String periodType;          // "1month", "3month", "6month", "1year", "custom"

    @SerializedName("custom_weeks")
    private int customWeeks;

    @SerializedName("running_days")
    private List<String> runningDays;   // ["mon", "wed", "fri"]

    @SerializedName("goal_distance_km")
    private float goalDistanceKm;

    @SerializedName("goal_pace_min_per_km")
    private float goalPaceMinPerKm;

    @SerializedName("start_date")
    private String startDate;           // "2026-04-30"

    public GoalRequest(int userId, String periodType, int customWeeks,
                       List<String> runningDays, float goalDistanceKm,
                       float goalPaceMinPerKm, String startDate) {
        this.userId = userId;
        this.periodType = periodType;
        this.customWeeks = customWeeks;
        this.runningDays = runningDays;
        this.goalDistanceKm = goalDistanceKm;
        this.goalPaceMinPerKm = goalPaceMinPerKm;
        this.startDate = startDate;
    }
}
