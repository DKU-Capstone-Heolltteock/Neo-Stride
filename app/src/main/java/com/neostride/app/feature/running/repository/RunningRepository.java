package com.neostride.app.feature.running.repository;

import android.util.Log;

import com.neostride.app.feature.running.api.RunningApi;
import com.neostride.app.feature.running.model.RunningRecordRequest;
import com.neostride.app.feature.running.model.RunningRecordResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RunningRepository {
    private final RunningApi runningApi;

    // 생성자: Api 인터페이스를 주입받음
    public RunningRepository(RunningApi runningApi) {
        this.runningApi = runningApi;
    }

    // 서버에 러닝 기록 저장 요청을 보내는 메서드
    public void saveRunningRecord(RunningRecordRequest request) {
        runningApi.saveRunningRecord(request).enqueue(new Callback<RunningRecordResponse>() {
            @Override
            public void onResponse(Call<RunningRecordResponse> call, Response<RunningRecordResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 저장 성공 시 로그 출력
                    Log.d("NeoStride_Network", "기록 저장 성공! ID: " + response.body().getRunRecordId());
                } else {
                    Log.e("NeoStride_Network", "저장 실패: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<RunningRecordResponse> call, Throwable t) {
                // 네트워크 에러 (인터넷 끊김 등)
                Log.e("NeoStride_Network", "네트워크 에러 발생: " + t.getMessage());
            }
        });
    }
}
