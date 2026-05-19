package com.neostride.app.feature.main.coaching;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;


//  코칭 플랜·히스토리 로컬 저장소
//  <p>
//  - SharedPreferences에 날짜별 {@link PlanData}와 {@link HistoryItem}을 Gson으로 직렬화하여 저장한다.
//  - {@link #saveGoalToPlanDays}로 목표 설정을 날짜별 플랜으로 펼쳐 저장한다.

public class GoalStorage {
    private static final String PREF_NAME = "neo_stride_goals";
    private static final String KEY_PLANS = "plans";
    private static final String KEY_HISTORY = "history";
    private static Gson gson = new Gson();

    private static SharedPreferences getPrefs(Context context) { return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); }

    // 날짜 키(예: "2026-5-4")에 해당하는 플랜을 저장한다.
    public static void savePlan(Context context, String dateKey, PlanData plan) {
        Map<String, PlanData> plans = getAllPlans(context);
        plans.put(dateKey, plan);
        getPrefs(context).edit().putString(KEY_PLANS, gson.toJson(plans)).apply();
    }

    public static PlanData getPlan(Context context, String dateKey) { return getAllPlans(context).get(dateKey); }

    public static Map<String, PlanData> getAllPlans(Context context) {
        String json = getPrefs(context).getString(KEY_PLANS, null);
        if (json == null) return new HashMap<>();
        Type type = new TypeToken<Map<String, PlanData>>(){}.getType();
        try { return gson.fromJson(json, type); } catch (Exception e) { return new HashMap<>(); }
    }

    // 지정 goalId에 속하는 날짜별 플랜을 모두 삭제한다.
    public static void removeAllPlansForGoal(Context context, String goalId) {
        Map<String, PlanData> plans = getAllPlans(context);
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, PlanData> entry : plans.entrySet()) { if (goalId.equals(entry.getValue().goalId)) keysToRemove.add(entry.getKey()); }
        for (String key : keysToRemove) plans.remove(key);
        getPrefs(context).edit().putString(KEY_PLANS, gson.toJson(plans)).apply();
    }

    public static void clearAllPlans(Context context) { getPrefs(context).edit().putString(KEY_PLANS, "{}").apply(); }

    // 히스토리 목록 맨 앞에 항목을 추가한다.
    public static void addHistory(Context context, HistoryItem item) {
        List<HistoryItem> history = getHistory(context);
        history.add(0, item);
        getPrefs(context).edit().putString(KEY_HISTORY, gson.toJson(history)).apply();
    }

    public static List<HistoryItem> getHistory(Context context) {
        String json = getPrefs(context).getString(KEY_HISTORY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<HistoryItem>>(){}.getType();
        try { return gson.fromJson(json, type); } catch (Exception e) { return new ArrayList<>(); }
    }

    public static void removeHistory(Context context, int index) {
        List<HistoryItem> history = getHistory(context);
        if (index >= 0 && index < history.size()) { history.remove(index); getPrefs(context).edit().putString(KEY_HISTORY, gson.toJson(history)).apply(); }
    }


//      목표 입력 데이터를 날짜별 플랜으로 펼쳐 저장하고 생성된 goalId를 반환한다.
//
//      @param goal 사용자가 설정한 목표 입력 데이터
//      @return 생성된 goalId 문자열 (예: "goal_1714920000000")

    public static String saveGoalToPlanDays(Context context, GoalInputData goal) {
        // 1. 이번 목표 설정을 하나로 묶어줄 고유 ID를 생성합니다 (현재 시간 기반)
        String goalId = "goal_" + System.currentTimeMillis();

        // 2. 훈련 시작 날짜를 오늘로 설정합니다.
        Calendar start = Calendar.getInstance();

        // 3. 훈련 종료 날짜를 설정한 기간(weeks)만큼 뒤로 설정합니다.
        Calendar end = Calendar.getInstance();
        end.add(Calendar.WEEK_OF_YEAR, goal.durationWeeks);

        int idCounter = 1; // 각 날짜별 플랜에 붙일 일련번호입니다.

        // 4. 시작일부터 종료일까지 하루씩 넘기며 반복문을 돌립니다.
        while (start.before(end)) {
            // 현재 날짜의 요일을 가져옵니다 (1:일요일, 2:월요일...)
            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            // 숫자로 된 요일을 "sun", "mon" 같은 문자열 키로 변환합니다.
            String dayKey = getDayKey(dayOfWeek);

            // 5. 유저가 선택한 러닝 데이(요일)에 해당하는 날인지 검사합니다.
            if (goal.runningDays != null && goal.runningDays.contains(dayKey)) {
                // 저장소에 쓸 날짜 키 값을 생성합니다 (예: "2026-5-4")
                String key = start.get(Calendar.YEAR) + "-" + (start.get(Calendar.MONTH) + 1) + "-" + start.get(Calendar.DAY_OF_MONTH);

                // 6. 해당 날짜에 저장할 데이터 객체를 생성합니다.
                PlanData plan = new PlanData();
                plan.planId = idCounter++; // 일련번호 부여 후 1 증가
                plan.goalId = goalId;      // 생성한 고유 목표 ID 부여

                // --- [데이터 분리 저장] ---
                // A. 오늘의 훈련량: 나중에 AI가 수정할 수 있도록 현재 설정값을 넣습니다.
                plan.distanceKm = goal.distanceKm;
                plan.paceStr = String.format("%d:%02d/km", goal.paceSecPerKm / 60, goal.paceSecPerKm % 60);

                // B. 최종 목표치: 어떤 날짜를 눌러도 Your Setting에 고정될 값을 넣습니다.
                // 선생님이 말씀하신 "10km / 5:30"이 여기에 박제되어 저장됩니다.
                plan.totalGoalDistanceKm = goal.distanceKm;
                plan.totalGoalPaceStr = plan.paceStr;
                // ------------------------

                plan.status = "pending";        // 초기 상태는 '대기 중'
                plan.isAiMission = false;       // 직접 설정한 것이므로 AI 미션은 아님
                plan.durationWeeks = goal.durationWeeks; // 설정한 전체 기간 저장
                plan.runningDays = goal.runningDays;     // 설정한 요일 리스트 저장

                // 7. 완성된 데이터를 SharedPreferences 저장소에 날짜별로 저장합니다.
                savePlan(context, key, plan);
            }

            // 다음 날짜로 넘어갑니다.
            start.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 생성된 목표 ID를 반환하며 마무리합니다.
        return goalId;
    }

    // ─── Calendar.DAY_OF_WEEK 정수를 영문 요일 키 문자열로 변환 ───
    private static String getDayKey(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY: return "sun";
            case Calendar.MONDAY: return "mon";
            case Calendar.TUESDAY: return "tue";
            case Calendar.WEDNESDAY: return "wed";
            case Calendar.THURSDAY: return "thu";
            case Calendar.FRIDAY: return "fri";
            case Calendar.SATURDAY: return "sat";
            default: return "";
        }
    }

    // 날짜별 코칭 플랜 데이터 모델
    public static class PlanData {
        public int planId;
        public String goalId;
        public float distanceKm;            // 오늘의 훈련 거리 (km)
        public float totalGoalDistanceKm;   // 최종 목표 거리 (Your Setting 고정용)
        public String totalGoalPaceStr;     // 최종 목표 페이스 (Your Setting 고정용)
        public int paceSecPerKm;
        public boolean isAiMission = false; // AI 코칭 미션 여부
        public int durationWeeks;
        public List<String> runningDays;
        public String status;               // "pending" | "completed" | "missed"
        public String paceStr;
        public String description;
        public String aiFeedbackComment;
        public long completedElapsedSec = 0;  // 목표 완료 시 총 경과 시간(초) - 완료 화면 복원용

        /**
         * 저장된 status가 "pending"이더라도 해당 날짜가 이미 지났으면 "missed"를 반환한다.
         * 기록 탭 등에서 코칭 탭 재방문 없이도 올바른 dot 색상을 표시하기 위해 사용한다.
         *
         * @param dateKey "yyyy-M-d" 형식의 날짜 키 (GoalStorage의 저장 키와 동일)
         * @return "completed" | "missed" | "pending"
         */
        public String getEffectiveStatus(String dateKey) {
            if ("completed".equals(status)) return "completed";
            if ("pending".equals(status)) {
                try {
                    String[] parts = dateKey.split("-");
                    java.time.LocalDate planDate = java.time.LocalDate.of(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]));
                    if (planDate.isBefore(java.time.LocalDate.now())) return "missed";
                } catch (Exception e) { /* 파싱 실패 시 원래 status 반환 */ }
            }
            return status != null ? status : "pending";
        }
    }

    // 목표 입력 데이터 모델 (목표 설정 화면에서 사용)
    public static class GoalInputData {
        public int durationWeeks;
        public List<String> runningDays;
        public float distanceKm;
        public int paceSecPerKm;
    }

    // 완료·삭제된 목표 히스토리 항목
    public static class HistoryItem {
        public String goalId;
        public float distanceKm;
        public String paceStr;
        public int durationWeeks;
        public String runningDaysStr;
        public String result;       // "completed" | "deleted"
        public long timestamp;
    }
}