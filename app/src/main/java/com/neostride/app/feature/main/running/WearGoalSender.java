package com.neostride.app.feature.main.running;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import com.neostride.app.feature.main.coaching.GoalStorage;

import java.util.Calendar;

// 오늘의 코칭 목표를 Wearable DataLayer로 워치에 전송하는 유틸 클래스
//
// RunningFragment.onResume()에서 호출한다.
// - 오늘 플랜이 있고 미완료 상태 → /coaching_goal DataItem 전송
// - 오늘 플랜 없거나 이미 완료   → /coaching_goal DataItem 삭제
//
// 워치(WearRunningActivity)는 onCreate()에서 이 DataItem을 읽어
// 코칭 모드 버튼 표시 여부를 결정한다.

public class WearGoalSender {

    private static final String TAG = "WearGoalSender";
    static final String PATH_COACHING_GOAL = "/coaching_goal";

    public static void sendTodayGoal(Context context) {
        Calendar today = Calendar.getInstance();
        String todayKey = today.get(Calendar.YEAR) + "-"
                + (today.get(Calendar.MONTH) + 1) + "-"
                + today.get(Calendar.DAY_OF_MONTH);

        GoalStorage.PlanData plan = GoalStorage.getPlan(context, todayKey);

        if (plan == null || "completed".equals(plan.status)) {
            // 오늘 플랜 없음 or 이미 완료 → 워치에서 코칭 버튼 숨기도록 DataItem 삭제
            Wearable.getDataClient(context)
                    .deleteDataItems(Uri.parse("wear://*" + PATH_COACHING_GOAL))
                    .addOnSuccessListener(i -> Log.d(TAG, "코칭 목표 DataItem 삭제 완료"))
                    .addOnFailureListener(e -> Log.w(TAG, "코칭 목표 DataItem 삭제 실패(무시 가능): " + e.getMessage()));
            return;
        }

        PutDataMapRequest putRequest = PutDataMapRequest.create(PATH_COACHING_GOAL);
        putRequest.getDataMap().putFloat("distance_km", plan.distanceKm);
        putRequest.getDataMap().putInt("pace_sec_per_km", plan.paceSecPerKm);
        // 동일 값을 재전송해도 DataLayer가 변경 감지하도록 timestamp 포함
        putRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());

        PutDataRequest request = putRequest.asPutDataRequest().setUrgent();

        Wearable.getDataClient(context).putDataItem(request)
                .addOnSuccessListener(item ->
                        Log.d(TAG, "코칭 목표 워치 전송 성공: " + plan.distanceKm + "km"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "코칭 목표 워치 전송 실패: " + e.getMessage()));
    }
}