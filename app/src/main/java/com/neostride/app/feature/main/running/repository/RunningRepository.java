package com.neostride.app.feature.main.running.repository;

import android.util.Log;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.main.running.api.RunningApi;
import com.neostride.app.feature.main.running.model.RunningRecordRequest;
import com.neostride.app.feature.main.running.model.RunningRecordResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;


//  러닝 기록 데이터 레포지터리
//  <p>
//  - 기록 저장·전체 조회·월별 조회·상세 조회를 서버에 위임하고 콜백으로 결과를 전달한다.

public class RunningRepository {

    private static final String TAG = "RunningRepository";
    private final RunningApi runningApi;

    // 제네릭 결과 콜백 인터페이스
    public interface OnResultListener<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    // 전체 기록 목록 조회 콜백 (MonthPageFragment에서 사용)
    public interface RecordCallback {
        void onSuccess(List<RunningRecordResponse> records);
        void onError(String message);
    }

    // 기본 생성자 — ApiClient 싱글톤에서 RunningApi 생성
    public RunningRepository() {
        this.runningApi = ApiClient.getInstance().create(RunningApi.class);
    }

    // 의존성 주입 생성자 (테스트 또는 기존 코드 호환용)
    public RunningRepository(RunningApi runningApi) {
        this.runningApi = runningApi;
    }

    // 러닝 기록을 저장한다 (콜백 없이 호출 시 결과 무시)
    public void saveRunningRecord(RunningRecordRequest request) {
        saveRunningRecord(request, null);
    }

    // 러닝 기록을 저장하고 결과를 listener로 전달한다
    public void saveRunningRecord(RunningRecordRequest request, OnResultListener<RunningRecordResponse> listener) {
        runningApi.saveRunningRecord(request).enqueue(new Callback<RunningRecordResponse>() {
            @Override
            public void onResponse(Call<RunningRecordResponse> call, Response<RunningRecordResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "기록 저장 성공! ID: " + response.body().getRunRecordId());
                    if (listener != null) listener.onSuccess(response.body());
                } else {
                    String msg = "저장 실패: " + response.code();

                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();

                            Log.e(TAG, "저장 실패 code: " + response.code());
                            Log.e(TAG, "저장 실패 errorBody: " + errorBody);

                            msg += " / " + errorBody;
                        } else {
                            Log.e(TAG, "저장 실패 code: " + response.code());
                            Log.e(TAG, "저장 실패 errorBody 없음");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "errorBody 읽기 실패: " + e.getMessage());
                    }

                    if (listener != null) listener.onError(msg);
                }
            }

            @Override
            public void onFailure(Call<RunningRecordResponse> call, Throwable t) {
                String msg = "네트워크 에러: " + t.getMessage();
                Log.e(TAG, msg, t);
                if (listener != null) listener.onError(msg);
            }
        });
    }

    // 사용자의 전체 러닝 기록을 조회해 RecordCallback으로 반환한다
    public void fetchUserRecords(int userId, RecordCallback callback) {
        runningApi.fetchUserRecords(userId).enqueue(new Callback<List<RunningRecordResponse>>() {
            @Override
            public void onResponse(Call<List<RunningRecordResponse>> call, Response<List<RunningRecordResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("기록 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<RunningRecordResponse>> call, Throwable t) {
                callback.onError("네트워크 에러: " + t.getMessage());
            }
        });
    }

    // 연·월 기준 러닝 기록을 조회해 OnResultListener로 반환한다
    public void getMonthlyRecords(int year, int month, OnResultListener<List<RunningRecordResponse>> listener) {
        runningApi.getMonthlyRecords(year, month).enqueue(new Callback<List<RunningRecordResponse>>() {
            @Override
            public void onResponse(Call<List<RunningRecordResponse>> call, Response<List<RunningRecordResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onSuccess(response.body());
                } else {
                    listener.onError("기록 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<RunningRecordResponse>> call, Throwable t) {
                listener.onError("네트워크 에러: " + t.getMessage());
            }
        });
    }

    // 특정 기록 ID의 상세 정보를 조회해 OnResultListener로 반환한다
    public void getRecordDetail(int recordId, OnResultListener<RunningRecordResponse> listener) {
        runningApi.getRecordDetail(recordId).enqueue(new Callback<RunningRecordResponse>() {
            @Override
            public void onResponse(Call<RunningRecordResponse> call, Response<RunningRecordResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onSuccess(response.body());
                } else {
                    listener.onError("상세 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RunningRecordResponse> call, Throwable t) {
                listener.onError("네트워크 에러: " + t.getMessage());
            }
        });
    }

    // 특정 러닝 기록을 삭제하고 결과를 OnResultListener로 반환한다
    public void deleteRecord(long recordId, OnResultListener<Void> listener) {
        runningApi.deleteRecord(recordId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "기록 삭제 성공! ID: " + recordId);
                    if (listener != null) listener.onSuccess(null);
                } else {
                    String msg = "삭제 실패: " + response.code();
                    Log.e(TAG, msg);
                    if (listener != null) listener.onError(msg);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String msg = "네트워크 에러: " + t.getMessage();
                Log.e(TAG, msg, t);
                if (listener != null) listener.onError(msg);
            }
        });
    }
}