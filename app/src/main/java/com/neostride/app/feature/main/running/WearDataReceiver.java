package com.neostride.app.feature.main.running;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.main.running.model.GpsTraceRequest;
import com.neostride.app.feature.main.running.model.RunningRecordRequest;
import com.neostride.app.feature.main.running.repository.RunningRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WearDataReceiver implements DataClient.OnDataChangedListener {

    private static final String TAG = "WearDataReceiver";
    private static final String PATH_RUNNING_RESULT = "/running_result";

    private final Context context;
    private final RunningRepository runningRepository;

    public WearDataReceiver(Context context) {
        this.context = context;
        this.runningRepository = new RunningRepository();
    }

    // 리스너 등록
    public void register() {
        Wearable.getDataClient(context).addListener(this);
        Log.d(TAG, "WearDataReceiver 등록됨");
    }

    // 리스너 해제
    public void unregister() {
        Wearable.getDataClient(context).removeListener(this);
        Log.d(TAG, "WearDataReceiver 해제됨");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() != DataEvent.TYPE_CHANGED) continue;
            if (!event.getDataItem().getUri().getPath().equals(PATH_RUNNING_RESULT)) continue;

            DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

            float distanceKm = dataMap.getFloat("distance_km");
            int durationSec = dataMap.getInt("duration_sec");
            int paceSecPerKm = dataMap.getInt("pace_sec_per_km");
            String gpsTracesJson = dataMap.getString("gps_traces");

            Log.d(TAG, "워치 데이터 수신 — 거리: " + distanceKm + "km, 시간: " + durationSec + "초");

            // GPS 좌표 파싱
            List<GpsTraceRequest> gpsTraces = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(gpsTracesJson);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    double lat = obj.getDouble("latitude");
                    double lng = obj.getDouble("longitude");
                    long timeMillis = Long.parseLong(obj.getString("time"));
                    String timeStr = sdf.format(new Date(timeMillis));
                    gpsTraces.add(new GpsTraceRequest(lat, lng, timeStr));
                }
            } catch (Exception e) {
                Log.e(TAG, "GPS 파싱 실패: " + e.getMessage());
            }

            // 칼로리 계산 (간단히 MET 공식: 체중 70kg 기준)
            float calories = distanceKm * 70 * 1.036f;

            // 서버에 저장
            int userId = TokenManager.getUserId(context);
            RunningRecordRequest request = new RunningRecordRequest(
                    userId,
                    null,           // 워치는 코칭 모드 없음
                    distanceKm,
                    durationSec,
                    paceSecPerKm,
                    calories,
                    "watch",        // 워치에서 온 기록임을 표시
                    gpsTraces,
                    null
            );

            runningRepository.saveRunningRecord(request, new RunningRepository.OnResultListener<>() {
                @Override
                public void onSuccess(com.neostride.app.feature.main.running.model.RunningRecordResponse data) {
                    Log.d(TAG, "워치 러닝 기록 서버 저장 성공!");
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "워치 러닝 기록 서버 저장 실패: " + message);
                }
            });
        }
    }
}