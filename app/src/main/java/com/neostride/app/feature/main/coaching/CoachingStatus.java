package com.neostride.app.feature.main.coaching;


//  코칭 목표의 최종 상태 열거형
//  <p>
//  - ACTIVE: 진행 중 (is_active: true)
//  - COMPLETED_SUCCESS: 성공적 완료 (is_active: false, is_achieved: true)
//  - COMPLETED_FAIL: 성적 미달 완료 (is_active: false, is_achieved: false) → 빨간 점 대상

public enum CoachingStatus {
    ACTIVE,
    COMPLETED_SUCCESS,
    COMPLETED_FAIL
}
