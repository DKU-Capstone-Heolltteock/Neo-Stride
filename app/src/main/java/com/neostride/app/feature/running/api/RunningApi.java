package com.neostride.app.feature.running.api;

import com.neostride.app.feature.running.model.RunningRecordRequest;
import com.neostride.app.feature.running.model.RunningRecordResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;


//  러닝 기록 API 인터페이스
//  <p>
//  - 러닝 기록 저장·조회·상세를 담당한다.
public interface RunningApi {

    // 러닝 기록 저장
    @POST("/api/running/records")
    Call<RunningRecordResponse> saveRunningRecord(@Body RunningRecordRequest request);

    // 특정 사용자의 전체 러닝 기록 조회
    @GET("/api/running/records/user/{user_id}")
    Call<List<RunningRecordResponse>> fetchUserRecords(@Path("user_id") int userId);

    // 연·월 기준 러닝 기록 조회
    @GET("/api/running/records")
    Call<List<RunningRecordResponse>> getMonthlyRecords(
            @Query("year") int year,
            @Query("month") int month
    );

    // 특정 기록 ID의 상세 정보 조회
    @GET("/api/running/records/{record_id}")
    Call<RunningRecordResponse> getRecordDetail(@Path("record_id") int recordId);
}