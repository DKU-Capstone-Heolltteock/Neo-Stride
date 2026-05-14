package com.neostride.app.feature.feed.repository;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.feed.api.FeedApi;
import com.neostride.app.feature.feed.model.FeedUploadRequest;
import com.neostride.app.feature.feed.model.FeedUploadResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 * 피드 관련 데이터 처리를 담당하는 Repository 클래스임
 * 실제 서버 API를 통해 피드 목록 조회, 피드 업로드를 처리함
 */
public class FeedRepository {

    private final FeedApi feedApi;

    /*
     * FeedRepository 생성자임
     * ApiClient를 통해 FeedApi 객체를 생성함
     */
    public FeedRepository() {
        feedApi = ApiClient.getInstance().create(FeedApi.class);
    }

    /*
     * 피드 목록을 조회하는 함수임
     */
    public void getFeedList(RepositoryCallback<List<FeedUploadResponse>> callback) {
        feedApi.getFeedList().enqueue(new Callback<List<FeedUploadResponse>>() {
            @Override
            public void onResponse(
                    Call<List<FeedUploadResponse>> call,
                    Response<List<FeedUploadResponse>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("피드 목록 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<List<FeedUploadResponse>> call,
                    Throwable t
            ) {
                callback.onError("서버 연결 실패: " + t.getMessage());
            }
        });
    }

    /*
     * 피드 업로드를 처리하는 함수임
     */
    public void uploadFeed(
            FeedUploadRequest request,
            RepositoryCallback<FeedUploadResponse> callback
    ) {
        feedApi.uploadFeed(request).enqueue(new Callback<FeedUploadResponse>() {
            @Override
            public void onResponse(
                    Call<FeedUploadResponse> call,
                    Response<FeedUploadResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("피드 업로드 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<FeedUploadResponse> call,
                    Throwable t
            ) {
                callback.onError("서버 연결 실패: " + t.getMessage());
            }
        });
    }

    /*
     * 태그된 사용자 목록을 조회하는 함수임
     * 현재 서버에 태그 조회 API가 없으면 빈 리스트를 반환함
     */
    public void getTaggedUsers(
            Long feedId,
            RepositoryCallback<List<String>> callback
    ) {
        callback.onSuccess(new ArrayList<>());
    }

    /*
     * Repository 작업 결과를 화면으로 전달하기 위한 Callback 인터페이스임
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }
}