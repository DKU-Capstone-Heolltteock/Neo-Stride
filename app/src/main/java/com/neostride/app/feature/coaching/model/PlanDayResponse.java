package com.neostride.app.feature.coaching.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;


//  날짜별 AI 플랜 DTO
//  ERD: COACHING_PLAN_DAYS 테이블 매핑

public class PlanDayResponse implements Serializable {

    @SerializedName("plan_day_id")
    private int planDayId;

    @SerializedName("plan_date")
    private String planDate;                // "2026-05-05"

    @SerializedName("day_distance_km")
    private float dayDistanceKm;

    @SerializedName("day_pace_min_per_km")
    private float dayPaceMinPerKm;

    @SerializedName("description")
    private String description;             // AI가 생성한 당일 코멘트

    @SerializedName("is_completed")
    private boolean isCompleted;

    @SerializedName("ai_feedback_comment")
    private String aiFeedbackComment;       // 러닝 완료 후 AI 피드백

    @SerializedName("ai_feedback_at")
    private String aiFeedbackAt;

    // Getter
    public int getPlanDayId() { return planDayId; }
    public String getPlanDate() { return planDate; }
    public float getDayDistanceKm() { return dayDistanceKm; }
    public float getDayPaceMinPerKm() { return dayPaceMinPerKm; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return isCompleted; }
    public String getAiFeedbackComment() { return aiFeedbackComment; }
    public String getAiFeedbackAt() { return aiFeedbackAt; }

    // 편의 메서드
    public String getFormattedPace() {
        int min = (int) dayPaceMinPerKm;
        int sec = (int) ((dayPaceMinPerKm - min) * 60);
        return String.format("%d:%02d/km", min, sec);
    }

    public String getFormattedDistance() {
        return String.format("%.1fkm", dayDistanceKm);
    }


//      플랜 상태 반환
//      완료: "completed"
//      날짜 지남 + 미완료: "missed"
//      아직 안 됨: "pending"

    public String getStatus() {
        if (isCompleted) return "completed";
        try {
            java.time.LocalDate planLocalDate = java.time.LocalDate.parse(planDate);
            if (planLocalDate.isBefore(java.time.LocalDate.now())) return "missed";
        } catch (Exception e) { /* ignore */ }
        return "pending";
    }
}