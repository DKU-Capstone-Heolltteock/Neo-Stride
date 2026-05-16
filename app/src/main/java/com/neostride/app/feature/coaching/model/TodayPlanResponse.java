package com.neostride.app.feature.coaching.model;

import com.google.gson.annotations.SerializedName;



//  오늘 플랜 조회 응답 DTO
//  <p>
//  GET /api/coaching/plans/today — 러닝 탭에서 코칭 버튼 표시 여부 판단에도 사용된다.

public class TodayPlanResponse {

    @SerializedName("has_plan")
    private boolean hasPlan;

    @SerializedName("plan_day")
    private PlanDayResponse planDay;

    @SerializedName("goal")
    private GoalResponse.GoalInfo goal;

    public boolean hasPlan() { return hasPlan; }
    public PlanDayResponse getPlanDay() { return planDay; }
    public GoalResponse.GoalInfo getGoal() { return goal; }
}