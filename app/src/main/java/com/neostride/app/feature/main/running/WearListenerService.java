package com.neostride.app.feature.main.running;

import android.util.Log;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

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

public class WearListenerService extends WearableListenerService {

    private static final String TAG = "WearListenerService";
    private static final String PATH_RUNNING_RESULT = "/running_result";

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

            float calories = distanceKm * 70 * 1.036f;
            int userId = TokenManager.getUserId(this);

            RunningRecordRequest request = new RunningRecordRequest(
                    userId,
                    null,
                    distanceKm,
                    durationSec,
                    paceSecPerKm,
                    calories,
                    "watch",
                    gpsTraces,
                    null
            );

            RunningRepository runningRepository = new RunningRepository();
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