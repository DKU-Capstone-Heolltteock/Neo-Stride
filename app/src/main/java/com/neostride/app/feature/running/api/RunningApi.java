package com.neostride.app.feature.running.api;

import com.neostride.app.feature.running.model.RunningRecordRequest;
import com.neostride.app.feature.running.model.RunningRecordResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RunningApi {
    // 러닝 기록 저장 (POST 방식)
    @POST("/api/running/records")
    Call<RunningRecordResponse> saveRunningRecord(@Body RunningRecordRequest request);
}