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

/*
 * 워치에서 전송한 러닝 결과를 폰 앱에서 수신하는 서비스임
 *
 * 전체 흐름:
 * 1. 워치 WearDataSender가 /running_result 경로로 DataItem 전송함
 * 2. 폰의 WearListenerService가 해당 데이터를 수신함
 * 3. 워치에서 받은 거리, 시간, 페이스, GPS 데이터를 RunningRecordRequest로 변환함
 * 4. RunningRepository를 통해 서버에 러닝 기록을 저장함
 *
 * 주의:
 * MainActivity에서 WearDataReceiver를 별도로 등록하면 중복 저장될 수 있으므로
 * 워치 데이터 수신은 이 서비스 하나만 사용하는 것을 권장함
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

            Log.d(TAG, "워치 데이터 수신 성공");
            Log.d(TAG, "distanceKm = " + distanceKm);
            Log.d(TAG, "durationSec = " + durationSec);
            Log.d(TAG, "paceSecPerKm = " + paceSecPerKm);
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

            RunningRecordRequest request = new RunningRecordRequest(
                    userId,
                    null,
                    distanceKm,
                    durationSec,
                    paceSecPerKm,
                    calories,
                    "",
                    gpsTraces,
                    null
            );
            saveWatchRunningRecord(request, event.getDataItem().getUri());
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
    private void saveWatchRunningRecord(RunningRecordRequest request, Uri dataItemUri) {
        RunningRepository runningRepository = new RunningRepository();

        runningRepository.saveRunningRecord(request, new RunningRepository.OnResultListener<RunningRecordResponse>() {
            @Override
            public void onSuccess(RunningRecordResponse data) {
                Log.d(TAG, "워치 러닝 기록 서버 저장 성공!");

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