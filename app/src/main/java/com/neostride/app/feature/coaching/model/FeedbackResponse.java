package com.neostride.app.feature.coaching.model;

import com.google.gson.annotations.SerializedName;



//  AI 피드백 응답 DTO
//  <p>
//  - COACHING_PLAN_DAYS.ai_feedback_comment, ai_feedback_at 필드에 매핑된다.

public class FeedbackResponse {

    @SerializedName("plan_day_id")
    private int planDayId;

    @SerializedName("is_completed")
    private boolean isCompleted;

    @SerializedName("ai_feedback_comment")
    private String aiFeedbackComment;

    @SerializedName("ai_feedback_at")
    private String aiFeedbackAt;

    public int getPlanDayId() { return planDayId; }
    public boolean isCompleted() { return isCompleted; }
    public String getAiFeedbackComment() { return aiFeedbackComment; }
    public String getAiFeedbackAt() { return aiFeedbackAt; }
}