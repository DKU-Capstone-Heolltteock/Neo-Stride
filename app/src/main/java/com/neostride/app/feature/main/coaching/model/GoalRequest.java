package com.neostride.app.feature.main.coaching.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;



//  목표 생성 요청 DTO
//  <p>
//  POST /api/coaching/goals — COACHING_PLANS 테이블에 저장될 데이터

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

    // 부동소수점 오차 방지를 위해 "초 단위 정수"로 전송한다.
    // 예) 5분 30초 → 330
    @SerializedName("goal_pace_sec_per_km")
    private int goalPaceSecPerKm;

    @SerializedName("start_date")
    private String startDate;           // "2026-04-30"

    public GoalRequest(int userId, String periodType, int customWeeks,
                       List<String> runningDays, float goalDistanceKm,
                       int goalPaceSecPerKm, String startDate) {
        this.userId = userId;
        this.periodType = periodType;
        this.customWeeks = customWeeks;
        this.runningDays = runningDays;
        this.goalDistanceKm = goalDistanceKm;
        this.goalPaceSecPerKm = goalPaceSecPerKm;
        this.startDate = startDate;
    }
}