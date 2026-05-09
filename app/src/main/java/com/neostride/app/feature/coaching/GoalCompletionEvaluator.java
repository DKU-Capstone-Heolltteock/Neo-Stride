package com.neostride.app.feature.coaching;

import com.neostride.app.feature.running.model.RunningRecordResponse;
import com.neostride.app.feature.coaching.model.GoalResponse;
import java.util.List;

public class GoalCompletionEvaluator {

    //  성공률 기준 (2/3)
    private static final float SUCCESS_THRESHOLD = 2.0f / 3.0f;

    /**
     * 현재 기록을 바탕으로 목표 상태를 판별합니다.
     */
    public static CoachingStatus evaluate(
            RunningRecordResponse latestRun,      // 방금 뛴 기록
            GoalResponse.GoalInfo goalInfo,       // 유저가 설정한 최종 목표 정보
            List<RunningRecordResponse> history   // 해당 목표 기간 전체 기록
    ) {
        // [조건 1] 조기 졸업 (Early Graduation)
        // 설정한 목표 거리와 페이스를 모두 뛰어넘었는가?
        if (latestRun.getDistance() >= goalInfo.getGoalDistanceKm() &&
                (latestRun.getPace() < 60 ? latestRun.getPace() : latestRun.getPace() / 60f) <= goalInfo.getGoalPaceMinPerKm()) {
            return CoachingStatus.COMPLETED_SUCCESS;
        }

        // [조건 2] 기간 만료 평가 (Final Evaluation)
        // 오늘이 계획된 훈련의 마지막 날인지 확인 (LocalDate 사용 권장)
        if (isLastDay(goalInfo.getEndDate())) {
            int totalDays = goalInfo.getDurationWeeks() * 7; // 대략적인 전체 일수
            int successCount = countSuccessDays(history, goalInfo);

            // 성공률이 $2/3$ 이상인가?
            if ((float) successCount / totalDays >= SUCCESS_THRESHOLD) {
                return CoachingStatus.COMPLETED_SUCCESS;
            } else {
                return CoachingStatus.COMPLETED_FAIL; //
            }
        }

        return CoachingStatus.ACTIVE; // 아직 목표 진행 중
    }

    private static boolean isLastDay(String endDate) {
        // 오늘 날짜와 endDate를 비교하는 로직 구현
        return false;
    }

    private static int countSuccessDays(List<RunningRecordResponse> history, GoalResponse.GoalInfo goal) {
        // 기록 중 '당일 목표'를 채운 횟수 카운트
        return 0;
    }
}