package com.neostride.app.feature.coaching.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;



//  날짜별 AI 플랜 응답 DTO
//  <p>
//  - ERD: COACHING_PLAN_DAYS 테이블과 매핑된다.
//  - {@link #getStatus()}로 완료/미완료/예정 상태를 판별한다.

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

    @SerializedName("actual_duration_sec")
    private int actualDurationSec;          // 실제 완료 시 소요된 총 시간(초) — 완료 화면에 -MM:SS 초과 표시용

    // Getter
    public int getPlanDayId() { return planDayId; }
    public String getPlanDate() { return planDate; }
    public float getDayDistanceKm() { return dayDistanceKm; }
    public float getDayPaceMinPerKm() { return dayPaceMinPerKm; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return isCompleted; }
    public String getAiFeedbackComment() { return aiFeedbackComment; }
    public String getAiFeedbackAt() { return aiFeedbackAt; }
    public int getActualDurationSec() { return actualDurationSec; }

    // 편의 메서드
    public String getFormattedPace() {
        int min = (int) dayPaceMinPerKm;
        int sec = (int) ((dayPaceMinPerKm - min) * 60);
        return String.format("%d:%02d/km", min, sec);
    }

    public String getFormattedDistance() {
        return String.format("%.1fkm", dayDistanceKm);
    }



//      플랜 상태 문자열을 반환한다.
//
//      @return "completed" (완료) | "missed" (날짜 경과·미완료) | "pending" (예정)

    public String getStatus() {
        if (isCompleted) return "completed";
        try {
            java.time.LocalDate planLocalDate = java.time.LocalDate.parse(planDate);
            if (planLocalDate.isBefore(java.time.LocalDate.now())) return "missed";
        } catch (Exception e) { /* ignore */ }
        return "pending";
    }
}