package com.neostride.app.feature.coaching.model;

import com.google.gson.annotations.SerializedName;
import com.neostride.app.feature.coaching.CoachingStatus;
import java.util.List;

public class GoalResponse {

    @SerializedName("goal_id")
    private int goalId;

    @SerializedName("has_active_goal")
    private boolean hasActiveGoal;

    @SerializedName("status")
    private String status; // 서버에서 오는 기본 문자열 상태값 ("active" 등)

    @SerializedName("goal")
    private GoalInfo goal;

    @SerializedName("plan_days")
    private List<PlanDayResponse> planDays;

    // --- Getter 메서드들 ---
    public int getGoalId() { return goalId; }
    public boolean hasActiveGoal() { return hasActiveGoal; }
    public String getStatus() { return status; } // 기존 String 반환 유지
    public GoalInfo getGoal() { return goal; }
    public List<PlanDayResponse> getPlanDays() { return planDays; }

    /**
     * 목표 기본 정보 (DB의 goals 테이블 데이터)
     */
    public static class GoalInfo {
        @SerializedName("goal_id")
        private int goalId;
        @SerializedName("period_type")
        private String periodType;
        @SerializedName("custom_weeks")
        private int customWeeks;
        @SerializedName("running_days")
        private List<String> runningDays; // 🌟 이 필드를 가져올 도구가 필요했습니다.
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

        // 🌟 백엔드 DB 스위치 (상태 판별용)
        @SerializedName("is_active")
        private boolean isActive;
        @SerializedName("is_achieved")
        private boolean isAchieved;

        // --- 🔍 보강된 Getter 메서드들 ---
        public int getGoalId() { return goalId; }
        public String getPeriodType() { return periodType; }
        public int getCustomWeeks() { return customWeeks; }
        public List<String> getRunningDays() { return runningDays; } // ✅ 에러 해결 포인트!
        public float getGoalDistanceKm() { return goalDistanceKm; }
        public float getGoalPaceMinPerKm() { return goalPaceMinPerKm; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public boolean isActive() { return isActive; }
        public boolean isAchieved() { return isAchieved; }

        /**
         * 🧪 [핵심] 성적 상태 판별 로직
         */
        public CoachingStatus getCoachingStatus() {
            if (isActive) return CoachingStatus.ACTIVE;
            if (isAchieved) return CoachingStatus.COMPLETED_SUCCESS;
            return CoachingStatus.COMPLETED_FAIL;
        }

        /**
         * 🌟 [UI용] 페이스 형식 변환 (예: 5.5 -> 5:30)
         */
        public String getFormattedPace() {
            int min = (int) goalPaceMinPerKm;
            int sec = (int) Math.round((goalPaceMinPerKm - min) * 60);
            return String.format(java.util.Locale.KOREA, "%d:%02d", min, sec);
        }

        public int getDurationWeeks() {
            if (customWeeks > 0) return customWeeks;
            switch (periodType != null ? periodType : "") {
                case "1month": return 4;
                case "3month": return 12;
                case "6month": return 24;
                case "1year": return 52;
                default: return 0;
            }
        }
    }
}