package com.neostride.wear;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataRequest;

public class WearDataSender {

    private static final String TAG = "WearDataSender";
    private static final String PATH_RUNNING_RESULT = "/running_result";

    // 러닝 완료 데이터를 폰으로 전송
    public static void sendRunningResult(Context context,
                                         float distanceKm,
                                         int durationSec,
                                         int paceSecPerKm,
                                         List<double[]> gpsPoints) {
        try {
            // GPS 좌표 JSON 변환
            JSONArray traceArray = new JSONArray();
            for (double[] point : gpsPoints) {
                JSONObject obj = new JSONObject();
                obj.put("latitude", point[0]);
                obj.put("longitude", point[1]);
                obj.put("time", (long) point[2] + "");
                traceArray.put(obj);
            }

            DataMap dataMap = new DataMap();
            dataMap.putFloat("distance_km", distanceKm);
            dataMap.putInt("duration_sec", durationSec);
            dataMap.putInt("pace_sec_per_km", paceSecPerKm);
            dataMap.putString("gps_traces", traceArray.toString());
            dataMap.putLong("timestamp", System.currentTimeMillis());

            PutDataRequest request = PutDataRequest.create(PATH_RUNNING_RESULT);
            request.setData(dataMap.toByteArray());

            Wearable.getDataClient(context).putDataItem(request)
                    .addOnSuccessListener(dataItem ->
                            Log.d(TAG, "폰으로 전송 성공"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "폰으로 전송 실패: " + e.getMessage()));

        } catch (Exception e) {
            Log.e(TAG, "데이터 변환 실패: " + e.getMessage());
        }
    }
}