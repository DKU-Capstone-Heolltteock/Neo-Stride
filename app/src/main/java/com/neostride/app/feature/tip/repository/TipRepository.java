package com.neostride.app.feature.tip.repository;

import android.util.Log;

import com.neostride.app.common.network.MockApiClient;
import com.neostride.app.feature.tip.api.TipApi;
import com.neostride.app.feature.tip.model.TipDetailResponse;
import com.neostride.app.feature.tip.model.TipResponse;
import com.neostride.app.feature.tip.model.TipUploadRequest;
import com.neostride.app.feature.tip.model.TipUploadResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 * 팁 Repository 클래스임
 * 팁 업로드, 팁 목록 조회, 팁 상세 조회 API 호출을 담당함
 */
public class TipRepository {

    private static final String TAG = "TipRepository";

    private final TipApi tipApi;

    /*
     * TipRepository 생성자임
     * 개발 중에는 MockApiClient를 사용해 목서버 응답을 받음
     * 실제 서버 연결 시에는 ApiClient로 되돌려야 함
     */
    public TipRepository() {
        tipApi = MockApiClient
                .getInstance()
                .create(TipApi.class);
    }

    /*
     * 팁 업로드 함수임
     * 사용자가 작성한 팁 데이터를 서버로 전송함
     */
    public void uploadTip(
            TipUploadRequest request,
            TipUploadCallback callback
    ) {
        tipApi.uploadTip(request)
                .enqueue(new Callback<TipUploadResponse>() {
                    @Override
                    public void onResponse(
                            Call<TipUploadResponse> call,
                            Response<TipUploadResponse> response
                    ) {
                        Log.d(TAG, "uploadTip response code = " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("팁 업로드 실패 / code = " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<TipUploadResponse> call,
                            Throwable t
                    ) {
                        Log.e(TAG, "uploadTip onFailure = " + t.getMessage());
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /*
     * 팁 목록 조회 함수임
     * 서버에서 팁 게시글 목록을 가져옴
     */
    public void getTips(
            TipListCallback callback
    ) {
        tipApi.getTips()
                .enqueue(new Callback<List<TipResponse>>() {
                    @Override
                    public void onResponse(
                            Call<List<TipResponse>> call,
                            Response<List<TipResponse>> response
                    ) {
                        Log.d(TAG, "getTips response code = " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("팁 조회 실패 / code = " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<TipResponse>> call,
                            Throwable t
                    ) {
                        Log.e(TAG, "getTips onFailure = " + t.getMessage());
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /*
     * 팁 상세 조회 함수임
     * tipId를 기준으로 서버에서 팁 상세 정보를 가져옴
     */
    public void getTipDetail(
            Long tipId,
            TipDetailCallback callback
    ) {
        tipApi.getTipDetail(tipId)
                .enqueue(new Callback<TipDetailResponse>() {
                    @Override
                    public void onResponse(
                            Call<TipDetailResponse> call,
                            Response<TipDetailResponse> response
                    ) {
                        Log.d(TAG, "getTipDetail response code = " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("팁 상세 조회 실패 / code = " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<TipDetailResponse> call,
                            Throwable t
                    ) {
                        Log.e(TAG, "getTipDetail onFailure = " + t.getMessage());
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /*
     * 팁 업로드 콜백 인터페이스임
     */
    public interface TipUploadCallback {
        void onSuccess(TipUploadResponse response);

        void onFailure(String message);
    }

    /*
     * 팁 목록 조회 콜백 인터페이스임
     */
    public interface TipListCallback {
        void onSuccess(List<TipResponse> response);

        void onFailure(String message);
    }

    /*
     * 팁 상세 조회 콜백 인터페이스임
     */
    public interface TipDetailCallback {
        void onSuccess(TipDetailResponse response);

        void onFailure(String message);
    }
}