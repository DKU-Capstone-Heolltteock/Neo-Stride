package com.neostride.app.feature.feed.repository;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.MockFeedServer;
import com.neostride.app.feature.feed.api.FeedApi;
import com.neostride.app.feature.feed.model.FeedUploadRequest;
import com.neostride.app.feature.feed.model.FeedUploadResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 * 피드 관련 데이터 처리를 담당하는 Repository 클래스임
 * Mock 모드일 때는 MockFeedServer를 사용하고,
 * 서버 모드일 때는 FeedApi를 사용함
 */
public class FeedRepository {

    /*
     * 현재는 서버 API가 완성되지 않았으므로 MockFeedServer를 사용함
     * 실제 서버 연결 시 false로 변경하면 됨
     */
    private static final boolean USE_MOCK = true;

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
        if (USE_MOCK) {
            callback.onSuccess(MockFeedServer.getFeedList());
            return;
        }

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
        if (USE_MOCK) {
            callback.onSuccess(MockFeedServer.uploadFeed(request));
            return;
        }

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
     * 현재는 MockFeedServer에서 가져옴
     */
    public void getTaggedUsers(
            Long feedId,
            RepositoryCallback<List<String>> callback
    ) {
        if (USE_MOCK) {
            callback.onSuccess(MockFeedServer.getTaggedUsers(feedId));
            return;
        }

        /*
         * 실제 서버 API가 생기면 아래처럼 변경하면 됨
         *
         * feedApi.getTaggedUsers(feedId).enqueue(...)
         */
        callback.onError("태그 조회 API가 아직 연결되지 않았습니다");
    }

    /*
     * Repository 작업 결과를 화면으로 전달하기 위한 Callback 인터페이스임
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }
}