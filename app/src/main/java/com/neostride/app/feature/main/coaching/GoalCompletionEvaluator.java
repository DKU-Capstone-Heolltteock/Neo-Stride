package com.neostride.app.feature.main.coaching;

import com.neostride.app.feature.main.coaching.model.GoalResponse;
import com.neostride.app.feature.main.coaching.model.PlanDayResponse;
import com.neostride.app.feature.main.running.model.RunningRecordResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 코칭 목표 달성 여부 평가 유틸리티 클래스
 * <p>
 * - 조기 졸업: 단일 러닝 기록이 최종 목표(거리·페이스)를 모두 충족하면 {@link CoachingStatus#COMPLETED_SUCCESS}
 * - 기간 만료: endDate 경과 후 완료율 ≥ 2/3이면 SUCCESS, 미달이면 {@link CoachingStatus#COMPLETED_FAIL}
 * - 평가 대상 아님: 아직 기간 중이고 조기 졸업도 아닌 경우 {@link CoachingStatus#ACTIVE}
 */
public class GoalCompletionEvaluator {

    /** 기간 만료 평가 시 성공으로 인정하는 최소 완료율 (2/3) */
    private static final float SUCCESS_THRESHOLD = 2.0f / 3.0f;

    /**
     * 현재 기록과 플랜 데이터를 바탕으로 목표 상태를 판별한다.
     *
     * @param latestRun  방금 완료한 러닝 기록 (조기 졸업 체크용, null 가능)
     * @param goalInfo   사용자가 설정한 최종 목표 정보
     * @param planDays   목표 기간 전체 날짜별 플랜 목록
     * @return 평가 결과 {@link CoachingStatus}
     */
    public static CoachingStatus evaluate(
            RunningRecordResponse latestRun,
            GoalResponse.GoalInfo goalInfo,
            List<PlanDayResponse> planDays
    ) {
        // [조건 1] 조기 졸업: 최신 기록이 최종 목표 거리 + 페이스를 동시에 충족
        if (latestRun != null) {
            boolean distOk = latestRun.getDistance() >= goalInfo.getGoalDistanceKm();
            // latestRun.getPace()는 분/km(float), goalInfo는 초/km(int) — 단위 맞춰 비교 (작을수록 빠름)
            float goalPaceMinPerKm = goalInfo.getGoalPaceSecPerKm() / 60f;
            boolean paceOk = latestRun.getPace() > 0 &&
                    latestRun.getPace() <= goalPaceMinPerKm;
            if (distOk && paceOk) {
                return CoachingStatus.COMPLETED_SUCCESS;
            }
        }

        // [조건 2] 기간 만료: endDate가 오늘 이전(혹은 오늘)인 경우 완료율 평가
        if (isEndDatePassed(goalInfo.getEndDate())) {
            int totalDays  = planDays != null ? planDays.size() : 0;
            int successCount = countCompletedDays(planDays);

            if (totalDays > 0 && (float) successCount / totalDays >= SUCCESS_THRESHOLD) {
                return CoachingStatus.COMPLETED_SUCCESS;
            } else {
                return CoachingStatus.COMPLETED_FAIL;
            }
        }

        // 아직 진행 중
        return CoachingStatus.ACTIVE;
    }

    /**
     * endDate 문자열("yyyy-MM-dd")이 오늘 날짜와 같거나 이전인지 확인한다.
     *
     * @param endDate "yyyy-MM-dd" 형식 종료일
     * @return 종료일이 오늘이거나 지났으면 true
     */
    private static boolean isEndDatePassed(String endDate) {
        if (endDate == null || endDate.isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            // endDate의 마지막 순간(자정) 이후인지 비교하기 위해 하루 추가
            Date end = sdf.parse(endDate);
            Date today = sdf.parse(sdf.format(new Date())); // 오늘 00:00:00
            return !today.before(end); // today >= end
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * planDays 목록 중 완료(isCompleted == true)인 항목의 수를 반환한다.
     *
     * @param planDays 전체 플랜 날짜 목록
     * @return 완료된 플랜 일 수
     */
    private static int countCompletedDays(List<PlanDayResponse> planDays) {
        if (planDays == null) return 0;
        int count = 0;
        for (PlanDayResponse day : planDays) {
            if (day.isCompleted()) count++;
        }
        return count;
    }
}