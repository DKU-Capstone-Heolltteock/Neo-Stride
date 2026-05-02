package com.neostride.app.feature.coaching;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// 목표/플랜 데이터를 SharedPreferences에 저장
// 탭 이동해도 데이터 유지됨
// 백엔드 연동 시 이 클래스를 API 호출로 교체하면 됨

public class GoalStorage {

    private static final String PREF_NAME = "neo_stride_goals";
    private static final String KEY_PLANS = "plans";        // 날짜별 플랜
    private static final String KEY_HISTORY = "history";    // 완료/삭제된 목표 히스토리

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static Gson gson = new Gson();

    // ── 플랜 저장/조회 ──

    public static void savePlan(Context context, String dateKey, PlanData plan) {
        Map<String, PlanData> plans = getAllPlans(context);
        plans.put(dateKey, plan);
        String json = gson.toJson(plans);
        getPrefs(context).edit().putString(KEY_PLANS, json).apply();
    }

    public static PlanData getPlan(Context context, String dateKey) {
        Map<String, PlanData> plans = getAllPlans(context);
        return plans.get(dateKey);
    }

    public static Map<String, PlanData> getAllPlans(Context context) {
        String json = getPrefs(context).getString(KEY_PLANS, null);
        if (json == null) return new HashMap<>();
        Type type = new TypeToken<Map<String, PlanData>>(){}.getType();
        try {
            Map<String, PlanData> map = gson.fromJson(json, type);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static void removePlan(Context context, String dateKey) {
        Map<String, PlanData> plans = getAllPlans(context);
        plans.remove(dateKey);
        String json = gson.toJson(plans);
        getPrefs(context).edit().putString(KEY_PLANS, json).apply();
    }

    // 특정 goalId에 해당하는 모든 플랜 삭제
    public static void removeAllPlansForGoal(Context context, String goalId) {
        Map<String, PlanData> plans = getAllPlans(context);
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, PlanData> entry : plans.entrySet()) {
            if (goalId.equals(entry.getValue().goalId)) {
                keysToRemove.add(entry.getKey());
            }
        }
        for (String key : keysToRemove) {
            plans.remove(key);
        }
        String json = gson.toJson(plans);
        getPrefs(context).edit().putString(KEY_PLANS, json).apply();
    }

    // 모든 플랜 초기화 (서버 동기화 시 사용)
    public static void clearAllPlans(Context context) {
        getPrefs(context).edit().putString(KEY_PLANS, "{}").apply();
    }

    // ── 히스토리 저장/조회 ──

    public static void addHistory(Context context, HistoryItem item) {
        List<HistoryItem> history = getHistory(context);
        history.add(0, item); // 최신이 위로
        String json = gson.toJson(history);
        getPrefs(context).edit().putString(KEY_HISTORY, json).apply();
    }

    public static List<HistoryItem> getHistory(Context context) {
        String json = getPrefs(context).getString(KEY_HISTORY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<HistoryItem>>(){}.getType();
        try {
            List<HistoryItem> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void removeHistory(Context context, int index) {
        List<HistoryItem> history = getHistory(context);
        if (index >= 0 && index < history.size()) {
            history.remove(index);
            String json = gson.toJson(history);
            getPrefs(context).edit().putString(KEY_HISTORY, json).apply();
        }
    }

    // ── 목표를 날짜별 플랜으로 저장 ──

    public static String saveGoalToPlanDays(Context context, GoalInputData goal) {
        String goalId = "goal_" + System.currentTimeMillis();

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.WEEK_OF_YEAR, goal.durationWeeks);
        Calendar today = Calendar.getInstance();

        while (start.before(end)) {
            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            boolean isRunningDay = false;

            for (String d : goal.runningDays) {
                switch (d) {
                    case "sun": isRunningDay = dayOfWeek == Calendar.SUNDAY; break;
                    case "mon": isRunningDay = dayOfWeek == Calendar.MONDAY; break;
                    case "tue": isRunningDay = dayOfWeek == Calendar.TUESDAY; break;
                    case "wed": isRunningDay = dayOfWeek == Calendar.WEDNESDAY; break;
                    case "thu": isRunningDay = dayOfWeek == Calendar.THURSDAY; break;
                    case "fri": isRunningDay = dayOfWeek == Calendar.FRIDAY; break;
                    case "sat": isRunningDay = dayOfWeek == Calendar.SATURDAY; break;
                }
                if (isRunningDay) break;
            }

            if (isRunningDay) {
                int y = start.get(Calendar.YEAR);
                int m = start.get(Calendar.MONTH) + 1;
                int d = start.get(Calendar.DAY_OF_MONTH);
                String key = y + "-" + m + "-" + d;

                // 상태 결정: 과거 날짜는 missed, 오늘 이후는 pending
                String status;
                if (start.before(today) && !(start.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                        && start.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))) {
                    status = "missed";
                } else {
                    status = "pending";
                }

                PlanData plan = new PlanData();
                plan.goalId = goalId;
                plan.distanceKm = goal.distanceKm;
                plan.paceSecPerKm = goal.paceSecPerKm;
                plan.durationWeeks = goal.durationWeeks;
                plan.runningDays = goal.runningDays;
                plan.status = status; // pending / completed / missed
                plan.paceStr = formatPace(goal.paceSecPerKm);

                savePlan(context, key, plan);
            }

            start.add(Calendar.DAY_OF_MONTH, 1);
        }

        return goalId;
    }

    public static String formatPace(int totalSec) {
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%d:%02d", min, sec);
    }

    // ── 데이터 클래스 ──

    public static class PlanData {
        public String goalId;
        public float distanceKm;
        public int paceSecPerKm;
        public int durationWeeks;
        public List<String> runningDays;
        public String status; // "pending" (주황), "completed" (초록), "missed" (빨강)
        public String paceStr;
        public String description;         // AI가 생성한 당일 코멘트
        public String aiFeedbackComment;   // 러닝 완료 후 AI 피드백
    }

    public static class GoalInputData {
        public int durationWeeks;
        public List<String> runningDays;
        public float distanceKm;
        public int paceSecPerKm;
    }

    public static class HistoryItem {
        public String goalId;
        public float distanceKm;
        public String paceStr;
        public int durationWeeks;
        public String runningDaysStr;
        public String result; // "completed" or "deleted"
        public long timestamp;
    }
}