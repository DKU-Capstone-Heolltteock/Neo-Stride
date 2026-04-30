package com.neostride.app.feature.running.repository;

import android.util.Log;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.running.api.RunningApi;
import com.neostride.app.feature.running.model.RunningRecordRequest;
import com.neostride.app.feature.running.model.RunningRecordResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class RunningRepository {

    private static final String TAG = "RunningRepository";
    private final RunningApi runningApi;

    // 콜백 인터페이스
    public interface OnResultListener<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    // MonthPageFragment에서 쓰는 콜백 (RecordRepository.RecordCallback 대체)
    public interface RecordCallback {
        void onSuccess(List<RunningRecordResponse> records);
        void onError(String message);
    }

    // 기본 생성자
    public RunningRepository() {
        this.runningApi = ApiClient.getInstance().create(RunningApi.class);
    }

    // 주입 생성자 (기존 코드 호환)
    public RunningRepository(RunningApi runningApi) {
        this.runningApi = runningApi;
    }

    // ── 기록 저장 ──
    public void saveRunningRecord(RunningRecordRequest request) {
        saveRunningRecord(request, null);
    }

    public void saveRunningRecord(RunningRecordRequest request, OnResultListener<RunningRecordResponse> listener) {
        runningApi.saveRunningRecord(request).enqueue(new Callback<RunningRecordResponse>() {
            @Override
            public void onResponse(Call<RunningRecordResponse> call, Response<RunningRecordResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "기록 저장 성공! ID: " + response.body().getRunRecordId());
                    if (listener != null) listener.onSuccess(response.body());
                } else {
                    String msg = "저장 실패: " + response.code();
                    Log.e(TAG, msg);
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

    // ── 유저별 전체 기록 조회 (MonthPageFragment에서 사용) ──
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

    // ── 월별 기록 조회 ──
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

    // ── 기록 상세 조회 ──
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
}

//package com.neostride.app.feature.running.repository;
//
//import android.util.Log;
//
//import com.neostride.app.common.network.ApiClient;
//import com.neostride.app.feature.running.api.RunningApi;
//import com.neostride.app.feature.running.model.RunningRecordRequest;
//import com.neostride.app.feature.running.model.RunningRecordResponse;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//import java.util.List;
//
//public class RunningRepository {
//
//    private static final String TAG = "RunningRepository";
//    private final RunningApi runningApi;
//
//    // [임시] 팀장님이 생성해주신 더미 유저 ID
//    private static final int DUMMY_USER_ID = 1000000;
//
//    public interface OnResultListener<T> {
//        void onSuccess(T data);
//        void onError(String message);
//    }
//
//    public interface RecordCallback {
//        void onSuccess(List<RunningRecordResponse> records);
//        void onError(String message);
//    }
//
//    public RunningRepository() {
//        this.runningApi = ApiClient.getInstance().create(RunningApi.class);
//    }
//
//    public RunningRepository(RunningApi runningApi) {
//        this.runningApi = runningApi;
//    }
//
//    // ── 기록 저장 ──
//    public void saveRunningRecord(RunningRecordRequest request) {
//        saveRunningRecord(request, null);
//    }
//
//    public void saveRunningRecord(RunningRecordRequest request, OnResultListener<RunningRecordResponse> listener) {
//        // [테스트용] 서버에 저장하기 전, 요청 객체에 더미 유저 ID를 강제로 세팅합니다.
//        // RunningRecordRequest 클래스에 setUserId 메소드가 있다고 가정합니다.
//        request.setUserId(DUMMY_USER_ID);
//
//        runningApi.saveRunningRecord(request).enqueue(new Callback<RunningRecordResponse>() {
//            @Override
//            public void onResponse(Call<RunningRecordResponse> call, Response<RunningRecordResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    Log.d(TAG, "기록 저장 성공! ID: " + response.body().getRunRecordId());
//                    if (listener != null) listener.onSuccess(response.body());
//                } else {
//                    String msg = "저장 실패: " + response.code();
//                    Log.e(TAG, msg);
//                    if (listener != null) listener.onError(msg);
//                }
//            }
//
//            @Override
//            public void onFailure(Call<RunningRecordResponse> call, Throwable t) {
//                String msg = "네트워크 에러: " + t.getMessage();
//                Log.e(TAG, msg, t);
//                if (listener != null) listener.onError(msg);
//            }
//        });
//    }
//
//    // ── 유저별 전체 기록 조회 (더미 ID 강제 적용) ──
//    public void fetchUserRecords(int userId, RecordCallback callback) {
//        // [테스트용] 매개변수로 들어온 userId를 무시하고 DUMMY_USER_ID를 사용합니다.
//        runningApi.fetchUserRecords(DUMMY_USER_ID).enqueue(new Callback<List<RunningRecordResponse>>() {
//            @Override
//            public void onResponse(Call<List<RunningRecordResponse>> call, Response<List<RunningRecordResponse>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    callback.onSuccess(response.body());
//                } else {
//                    callback.onError("기록 조회 실패: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<List<RunningRecordResponse>> call, Throwable t) {
//                callback.onError("네트워크 에러: " + t.getMessage());
//            }
//        });
//    }
//
//    // ── 월별 기록 조회 ──
//    public void getMonthlyRecords(int year, int month, OnResultListener<List<RunningRecordResponse>> listener) {
//        runningApi.getMonthlyRecords(year, month).enqueue(new Callback<List<RunningRecordResponse>>() {
//            @Override
//            public void onResponse(Call<List<RunningRecordResponse>> call, Response<List<RunningRecordResponse>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    listener.onSuccess(response.body());
//                } else {
//                    listener.onError("기록 조회 실패: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<List<RunningRecordResponse>> call, Throwable t) {
//                listener.onError("네트워크 에러: " + t.getMessage());
//            }
//        });
//    }
//
//    // ── 기록 상세 조회 ──
//    public void getRecordDetail(int recordId, OnResultListener<RunningRecordResponse> listener) {
//        runningApi.getRecordDetail(recordId).enqueue(new Callback<RunningRecordResponse>() {
//            @Override
//            public void onResponse(Call<RunningRecordResponse> call, Response<RunningRecordResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    listener.onSuccess(response.body());
//                } else {
//                    listener.onError("상세 조회 실패: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<RunningRecordResponse> call, Throwable t) {
//                listener.onError("네트워크 에러: " + t.getMessage());
//            }
//        });
//    }
//}