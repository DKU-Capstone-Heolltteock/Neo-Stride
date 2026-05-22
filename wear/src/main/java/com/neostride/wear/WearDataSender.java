package com.neostride.wear;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class WearDataSender {

    private static final String TAG = "WearDataSender";
    private static final String PATH_RUNNING_RESULT = "/running_result";

    public static void sendRunningResult(Context context,
                                         float distanceKm,
                                         int durationSec,
                                         int paceSecPerKm,
                                         List<double[]> gpsPoints) {
        try {
            JSONArray traceArray = new JSONArray();
            for (double[] point : gpsPoints) {
                JSONObject obj = new JSONObject();
                obj.put("latitude", point[0]);
                obj.put("longitude", point[1]);
                obj.put("time", (long) point[2] + "");
                traceArray.put(obj);
            }

            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_RUNNING_RESULT);
            putDataMapRequest.getDataMap().putFloat("distance_km", distanceKm);
            putDataMapRequest.getDataMap().putInt("duration_sec", durationSec);
            putDataMapRequest.getDataMap().putInt("pace_sec_per_km", paceSecPerKm);
            putDataMapRequest.getDataMap().putString("gps_traces", traceArray.toString());
            putDataMapRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());

            PutDataRequest request = putDataMapRequest.asPutDataRequest().setUrgent();

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