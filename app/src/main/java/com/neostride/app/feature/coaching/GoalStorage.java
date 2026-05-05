package com.neostride.app.feature.coaching;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;

public class GoalStorage {
    private static final String PREF_NAME = "neo_stride_goals";
    private static final String KEY_PLANS = "plans";
    private static final String KEY_HISTORY = "history";
    private static Gson gson = new Gson();

    private static SharedPreferences getPrefs(Context context) { return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); }

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

    public static void removeAllPlansForGoal(Context context, String goalId) {
        Map<String, PlanData> plans = getAllPlans(context);
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, PlanData> entry : plans.entrySet()) { if (goalId.equals(entry.getValue().goalId)) keysToRemove.add(entry.getKey()); }
        for (String key : keysToRemove) plans.remove(key);
        getPrefs(context).edit().putString(KEY_PLANS, gson.toJson(plans)).apply();
    }

    public static void clearAllPlans(Context context) { getPrefs(context).edit().putString(KEY_PLANS, "{}").apply(); }

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

    // 이 부분을 찾아 아래 내용으로 교체하세요!
    public static class PlanData {
        public int planId;
        public String goalId;
        public float distanceKm;        // 오늘의 훈련 거리
        public float totalGoalDistanceKm; // 최종 목표 거리 (10km 고정용)
        public String totalGoalPaceStr;   // 최종 목표 페이스 (5:30 고정용)

        public int paceSecPerKm;
        public boolean isAiMission = false;
        public int durationWeeks;
        public List<String> runningDays;
        public String status;
        public String paceStr;
        public String description;
        public String aiFeedbackComment;
    }

    public static class GoalInputData { public int durationWeeks; public List<String> runningDays; public float distanceKm; public int paceSecPerKm; }
    public static class HistoryItem { public String goalId; public float distanceKm; public String paceStr; public int durationWeeks; public String runningDaysStr; public String result; public long timestamp; }
}