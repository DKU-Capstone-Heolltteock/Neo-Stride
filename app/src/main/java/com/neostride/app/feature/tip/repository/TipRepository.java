package com.neostride.app.feature.tip.repository;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.tip.api.TipApi;
import com.neostride.app.feature.tip.model.TipListResponse;
import com.neostride.app.feature.tip.model.TipUploadRequest;
import com.neostride.app.feature.tip.model.TipUploadResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 * 팁 Repository 클래스임
 */
public class TipRepository {

    private final TipApi tipApi;

    public TipRepository() {
        tipApi = ApiClient
                .getInstance()
                .create(TipApi.class);
    }

    /*
     * 팁 업로드 함수임
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
                        if (response.isSuccessful()
                                && response.body() != null) {

                            callback.onSuccess(response.body());

                        } else {
                            callback.onFailure("팁 업로드 실패");
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<TipUploadResponse> call,
                            Throwable t
                    ) {
                        callback.onFailure(t.getMessage());
                    }
                });
    }

    /*
     * 팁 목록 조회 함수임
     */
    public void getTips(
            TipListCallback callback
    ) {
        tipApi.getTips()
                .enqueue(new Callback<TipListResponse>() {
                    @Override
                    public void onResponse(
                            Call<TipListResponse> call,
                            Response<TipListResponse> response
                    ) {
                        if (response.isSuccessful()
                                && response.body() != null) {

                            callback.onSuccess(response.body());

                        } else {
                            callback.onFailure("팁 조회 실패");
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<TipListResponse> call,
                            Throwable t
                    ) {
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
        void onSuccess(TipListResponse response);
        void onFailure(String message);
    }
}