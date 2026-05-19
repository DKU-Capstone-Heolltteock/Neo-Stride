package com.neostride.app.feature.main.coaching.model;

import com.google.gson.annotations.SerializedName;


//  목표 상태 변경 요청 DTO
//  <p>
//  PATCH /api/coaching/goals/{goal_id}/status
//  <p>
//  - 조건 1 (조기 졸업): 목표 거리·페이스를 모두 달성한 경우
//  - 조건 2 (기간 만료): 종료일까지 출석률 2/3 이상인 경우

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
