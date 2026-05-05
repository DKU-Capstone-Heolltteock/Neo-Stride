package com.neostride.app.feature.coaching.model;

import com.google.gson.annotations.SerializedName;

// 목표 졸업(성공/실패) 처리를 위해 서버로 보낼 DTO
// 조건 1 : 조기졸업 ( 목표 거리에서 목표 페이스를 달성했을 경우)
// 조건 2 : 끝까지 달렸을때 출석일이 2/3 이상일 경우
// PATCH /api/coaching/goals/{goal_id}/status
public class GoalStatusUpdateRequest {

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("is_achieved")
    private boolean isAchieved;

    public GoalStatusUpdateRequest(boolean isActive, boolean isAchieved) {
        this.isActive = isActive;
        this.isAchieved = isAchieved;
    }
}
