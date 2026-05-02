package com.neostride.app.feature.coaching.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;


// 목표 생성/조회 응답 DTO
// ERD: COACHING_PLANS + COACHING_PLAN_DAYS 통합
public class GoalResponse {

    @SerializedName("goal_id")
    private int goalId;

    @SerializedName("has_active_goal")
    private boolean hasActiveGoal;

    @SerializedName("status")
    private String status;                  // "active", "completed", "deleted"

    // 목표 정보 (COACHING_PLANS)
    @SerializedName("goal")
    private GoalInfo goal;

    // AI가 생성한 날짜별 플랜 (COACHING_PLAN_DAYS)
    @SerializedName("plan_days")
    private List<PlanDayResponse> planDays;

    // Getter
    public int getGoalId() { return goalId; }
    public boolean hasActiveGoal() { return hasActiveGoal; }
    public String getStatus() { return status; }
    public GoalInfo getGoal() { return goal; }
    public List<PlanDayResponse> getPlanDays() { return planDays; }

    /**
     * 목표 기본 정보
     */
    public static class GoalInfo {
        @SerializedName("goal_id")
        private int goalId;

        @SerializedName("period_type")
        private String periodType;

        @SerializedName("custom_weeks")
        private int customWeeks;

        @SerializedName("running_days")
        private List<String> runningDays;

        @SerializedName("goal_distance_km")
        private float goalDistanceKm;

        @SerializedName("goal_pace_min_per_km")
        private float goalPaceMinPerKm;

        @SerializedName("start_date")
        private String startDate;

        @SerializedName("end_date")
        private String endDate;

        @SerializedName("created_at")
        private String createdAt;

        public int getGoalId() { return goalId; }
        public String getPeriodType() { return periodType; }
        public int getCustomWeeks() { return customWeeks; }
        public List<String> getRunningDays() { return runningDays; }
        public float getGoalDistanceKm() { return goalDistanceKm; }
        public float getGoalPaceMinPerKm() { return goalPaceMinPerKm; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }

        public String getFormattedPace() {
            int min = (int) goalPaceMinPerKm;
            int sec = (int) ((goalPaceMinPerKm - min) * 60);
            return String.format("%d:%02d", min, sec);
        }

        public int getDurationWeeks() {
            if (customWeeks > 0) return customWeeks;
            switch (periodType) {
                case "1month": return 4;
                case "3month": return 12;
                case "6month": return 24;
                case "1year": return 52;
                default: return customWeeks;
            }
        }
    }
}