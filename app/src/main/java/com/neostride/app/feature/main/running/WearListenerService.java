package com.neostride.app.feature.main.running;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.main.running.model.GpsTraceRequest;
import com.neostride.app.feature.main.running.model.RunningRecordRequest;
import com.neostride.app.feature.main.running.model.RunningRecordResponse;
import com.neostride.app.feature.main.running.repository.RunningRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.net.Uri;
import com.google.android.gms.wearable.Wearable;
import com.neostride.app.feature.main.coaching.GoalStorage;
import com.neostride.app.feature.main.coaching.repository.CoachingRepository;
import com.neostride.app.feature.main.coaching.model.FeedbackRequest;
import com.neostride.app.feature.main.coaching.model.FeedbackResponse;
import com.neostride.app.feature.main.coaching.model.GoalStatusUpdateRequest;
import java.util.Calendar;

/*
 * 워치에서 전송한 러닝 결과를 폰 앱에서 수신하는 서비스임
 *
 * 전체 흐름:
 * 1. 워치 WearDataSender가 /running_result/{run_id} 경로로 DataItem 전송함
 * 2. 폰의 WearListenerService가 해당 데이터를 수신함
 * 3. 워치에서 받은 거리, 시간, 페이스, GPS 데이터를 RunningRecordRequest로 변환함
 * 4. RunningRepository를 통해 서버에 러닝 기록을 저장함
 * 5. 서버 저장 성공 시 DataItem을 삭제해 재동기화로 인한 중복 저장을 방지함
 *
 * 워치 데이터 수신은 AndroidManifest에 등록된 이 서비스가 유일한 진입점임
 */
public class WearListenerService extends WearableListenerService {

    private static final String TAG = "WearListenerService";
    private static final String PATH_RUNNING_RESULT = "/running_result";

    @Override
    public void onCreate() {
        super.onCreate();

        /*
         * 이 서비스는 MainActivity와 별도로 실행될 수 있음
         * 따라서 서비스 내부에서도 ApiClient를 초기화해야 함
         */
        ApiClient.init(this);

        Log.d(TAG, "WearListenerService 생성됨");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        Log.d(TAG, "onDataChanged 호출됨");

        for (DataEvent event : dataEvents) {
            if (event.getType() != DataEvent.TYPE_CHANGED) {
                continue;
            }

            String path = event.getDataItem().getUri().getPath();
            Log.d(TAG, "수신된 DataItem path = " + path);

            if (path == null || !path.startsWith(PATH_RUNNING_RESULT)) {
                continue;
            }

            DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

            float distanceKm = dataMap.getFloat("distance_km", 0f);
            int durationSec = dataMap.getInt("duration_sec", 0);
            int paceSecPerKm = dataMap.getInt("pace_sec_per_km", 0);
            String gpsTracesJson = dataMap.getString("gps_traces", "[]");
            long timestamp = dataMap.getLong("timestamp", System.currentTimeMillis());
            long runId = dataMap.getLong("run_id", 0L);

            boolean isCoaching = dataMap.getBoolean("is_coaching", false);

            Log.d(TAG, "워치 데이터 수신 성공");
            Log.d(TAG, "distanceKm = " + distanceKm);
            Log.d(TAG, "durationSec = " + durationSec);
            Log.d(TAG, "paceSecPerKm = " + paceSecPerKm);
            Log.d(TAG, "isCoaching = " + isCoaching);
            Log.d(TAG, "gpsTracesJson = " + gpsTracesJson);
            Log.d(TAG, "timestamp = " + timestamp);
            Log.d(TAG, "runId = " + runId);
            List<GpsTraceRequest> gpsTraces = parseGpsTraces(gpsTracesJson);

            /*
             * 현재 워치 기록의 칼로리는 임시 계산값임
             * 70kg 기준 공식으로 계산함
             */
            float calories = distanceKm * 70f * 1.036f;

            /*
             * 로그인한 사용자 ID를 토큰 저장소에서 가져옴
             * 만약 0 또는 비정상 값이 나오면 로그인/토큰 저장 상태를 확인해야 함
             */
            int userId = TokenManager.getUserId(this);
            Log.d(TAG, "userId = " + userId);

            // 코칭 record라면 오늘 plan_day의 서버 planId를 가져와 같이 전송
            //  → 서버가 코칭 기록으로 인식하고, AI 피드백 요청도 정상 처리됨
            Integer planIdForRecord = null;
            if (isCoaching) {
                Calendar today = Calendar.getInstance();
                String todayKey = today.get(Calendar.YEAR) + "-"
                        + (today.get(Calendar.MONTH) + 1) + "-"
                        + today.get(Calendar.DAY_OF_MONTH);
                GoalStorage.PlanData todayPlan = GoalStorage.getPlan(this, todayKey);
                if (todayPlan != null && todayPlan.planId > 0) {
                    planIdForRecord = todayPlan.planId;
                    Log.d(TAG, "코칭 record — plan_day_id 첨부: " + planIdForRecord);
                }
            }

            RunningRecordRequest request = new RunningRecordRequest(
                    userId,
                    planIdForRecord,
                    distanceKm,
                    durationSec,
                    paceSecPerKm,
                    calories,
                    "",
                    gpsTraces,
                    null
            );
            saveWatchRunningRecord(request, event.getDataItem().getUri(), isCoaching, durationSec,
                    distanceKm, paceSecPerKm, planIdForRecord);
        }
    }

    /*
     * 워치에서 문자열 JSON으로 보낸 GPS 배열을 서버 저장용 DTO 리스트로 변환하는 함수임
     *
     * 워치 전송 예:
     * [
     *   {
     *     "latitude": 37.123,
     *     "longitude": 127.123,
     *     "time": "1760000000000"
     *   }
     * ]
     *
     * 서버 저장용 변환:
     * GpsTraceRequest(latitude, longitude, "yyyy-MM-dd'T'HH:mm:ss")
     */
    private List<GpsTraceRequest> parseGpsTraces(String gpsTracesJson) {
        List<GpsTraceRequest> gpsTraces = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(gpsTracesJson);

            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
            );

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                double latitude = obj.getDouble("latitude");
                double longitude = obj.getDouble("longitude");

                String rawTime = obj.optString("time", String.valueOf(System.currentTimeMillis()));
                long timeMillis = Long.parseLong(rawTime);

                String formattedTime = sdf.format(new Date(timeMillis));

                gpsTraces.add(new GpsTraceRequest(
                        latitude,
                        longitude,
                        formattedTime
                ));
            }

            Log.d(TAG, "GPS 파싱 성공, 개수 = " + gpsTraces.size());

        } catch (Exception e) {
            Log.e(TAG, "GPS 파싱 실패: " + e.getMessage());
        }

        return gpsTraces;
    }

    /*
     * 워치 러닝 기록을 서버에 저장하는 함수임
     */
    private void saveWatchRunningRecord(RunningRecordRequest request, Uri dataItemUri,
                                        boolean isCoaching, int durationSec,
                                        float distanceKm, int paceSecPerKm, Integer planDayId) {
        RunningRepository runningRepository = new RunningRepository();

        runningRepository.saveRunningRecord(request, new RunningRepository.OnResultListener<RunningRecordResponse>() {
            @Override
            public void onSuccess(RunningRecordResponse data) {
                Log.d(TAG, "워치 러닝 기록 서버 저장 성공!");

                // 코칭 모드 기록이면 오늘 플랜을 completed로 업데이트
                if (isCoaching) {
                    Calendar today = Calendar.getInstance();
                    String todayKey = today.get(Calendar.YEAR) + "-"
                            + (today.get(Calendar.MONTH) + 1) + "-"
                            + today.get(Calendar.DAY_OF_MONTH);
                    GoalStorage.PlanData plan = GoalStorage.getPlan(WearListenerService.this, todayKey);
                    if (plan != null) {
                        plan.status = "completed";
                        plan.completedElapsedSec = durationSec;
                        GoalStorage.savePlan(WearListenerService.this, todayKey, plan);
                        Log.d(TAG, "코칭 목표 completed 처리 완료: " + todayKey);

                        // 서버에도 완료 상태 반영 → CoachingFragment가 덮어쓰지 않도록
                        if (plan.goalId != null && plan.goalId.startsWith("goal_server_")) {
                            try {
                                int serverGoalId = Integer.parseInt(plan.goalId.replace("goal_server_", ""));
                                CoachingRepository coachingRepo = new CoachingRepository();
                                coachingRepo.updateGoalStatus(
                                        serverGoalId,
                                        new GoalStatusUpdateRequest(false, true),
                                        new CoachingRepository.OnResultListener<com.neostride.app.feature.main.coaching.model.GoalResponse>() {
                                            @Override
                                            public void onSuccess(com.neostride.app.feature.main.coaching.model.GoalResponse r) {
                                                Log.d(TAG, "서버 목표 상태 완료 처리 성공");
                                            }
                                            @Override
                                            public void onError(String message) {
                                                Log.e(TAG, "서버 목표 상태 완료 처리 실패: " + message);
                                            }
                                        }
                                );
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "goalId 파싱 실패: " + plan.goalId);
                            }
                        }
                    }

                    // AI 피드백 생성 요청 — plan_day_id 있을 때만 (폰 RunningFragment.requestAiFeedback과 동일 패턴)
                    if (planDayId != null && planDayId > 0) {
                        FeedbackRequest feedbackReq = new FeedbackRequest(
                                planDayId,
                                distanceKm,
                                durationSec,
                                paceSecPerKm / 60f   // 초/km → 분/km
                        );
                        new CoachingRepository().requestFeedback(planDayId, feedbackReq,
                                new CoachingRepository.OnResultListener<FeedbackResponse>() {
                                    @Override
                                    public void onSuccess(FeedbackResponse r) {
                                        Log.d(TAG, "워치 코칭 AI 피드백 생성 성공");
                                        // GoalStorage에 피드백 코멘트 저장 → 코칭 탭 진입 시 즉시 표시
                                        if (r.getAiFeedbackComment() != null) {
                                            String key = today.get(Calendar.YEAR) + "-"
                                                    + (today.get(Calendar.MONTH) + 1) + "-"
                                                    + today.get(Calendar.DAY_OF_MONTH);
                                            GoalStorage.PlanData p2 = GoalStorage.getPlan(WearListenerService.this, key);
                                            if (p2 != null) {
                                                p2.aiFeedbackComment = r.getAiFeedbackComment();
                                                GoalStorage.savePlan(WearListenerService.this, key, p2);
                                            }
                                        }
                                    }
                                    @Override
                                    public void onError(String message) {
                                        Log.e(TAG, "워치 코칭 AI 피드백 생성 실패: " + message);
                                    }
                                });
                    }
                }

                /*
                 * 서버 저장이 성공한 워치 DataItem을 삭제함
                 * 같은 워치 기록이 나중에 다시 동기화되어 중복 저장되는 것을 방지함
                 */
                Wearable.getDataClient(WearListenerService.this)
                        .deleteDataItems(dataItemUri)
                        .addOnSuccessListener(count ->
                                Log.d(TAG, "처리 완료 DataItem 삭제 성공, count = " + count)
                        )
                        .addOnFailureListener(e ->
                                Log.e(TAG, "처리 완료 DataItem 삭제 실패: " + e.getMessage())
                        );
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "워치 러닝 기록 서버 저장 실패: " + message);

                /*
                 * 서버 저장 실패 시에는 DataItem을 삭제하지 않음
                 * 네트워크/토큰 문제가 해결되면 나중에 다시 처리될 수 있도록 남겨둠
                 */
            }
        });
    }
}