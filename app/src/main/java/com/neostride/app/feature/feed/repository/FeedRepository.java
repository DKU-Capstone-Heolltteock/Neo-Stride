package com.neostride.app.feature.feed.repository;

import android.content.Context;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.MockApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.feed.api.FeedApi;
import com.neostride.app.feature.feed.model.FeedDetailResponse;
import com.neostride.app.feature.feed.model.FeedResponse;
import com.neostride.app.feature.feed.model.FeedUploadRequest;
import com.neostride.app.feature.feed.model.TagUser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 * 피드 관련 데이터 처리를 담당하는 Repository 클래스임
 * 실제 서버 API 또는 Mock API를 통해 피드 목록 조회, 피드 상세 조회, 피드 업로드를 처리함
 */
public class FeedRepository {

    private final FeedApi feedApi;

    // 로그인한 사용자 ID를 가져오기 위해 Context를 저장함
    private final Context context;

    /*
     * FeedRepository 생성자임
     * ApiClient 또는 MockApiClient를 통해 FeedApi 객체를 생성함
     * Context는 TokenManager에서 로그인한 사용자 ID를 가져오기 위해 사용함
     */
    public FeedRepository(Context context) {
        this.context = context.getApplicationContext();

        // 실제 서버 연결 시 사용함
        // feedApi = ApiClient.getInstance().create(FeedApi.class);

        // 개발 중 Mock 서버 테스트용으로 사용함
        // push/merge 전에는 실제 서버용 ApiClient로 되돌리는 것을 권장함
        feedApi = MockApiClient.getInstance().create(FeedApi.class);
    }

    /*
     * 피드 목록을 조회하는 함수임
     * GET /api/feeds 요청에는 X-User-Id 헤더가 필요함
     */
    public void getFeedList(RepositoryCallback<List<FeedResponse>> callback) {

        // 현재 로그인한 사용자 ID를 가져옴
        int userId = TokenManager.getUserId(context);

        // FeedApi의 getFeedList 함수에 X-User-Id 헤더 값으로 userId를 전달함
        feedApi.getFeedList((long) userId).enqueue(new Callback<List<FeedResponse>>() {
            @Override
            public void onResponse(
                    Call<List<FeedResponse>> call,
                    Response<List<FeedResponse>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("피드 목록 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<List<FeedResponse>> call,
                    Throwable t
            ) {
                callback.onError("서버 연결 실패: " + t.getMessage());
            }
        });
    }

    /*
     * 피드 상세 정보를 조회하는 함수임
     * GET /api/feeds/{feedId} 요청에는 X-User-Id 헤더가 필요함
     * 상세 화면 전용 FeedDetailResponse를 반환함
     */
    public void getFeedDetail(
            Long feedId,
            RepositoryCallback<FeedDetailResponse> callback
    ) {
        int userId = TokenManager.getUserId(context);

        feedApi.getFeedDetail((long) userId, feedId).enqueue(new Callback<FeedDetailResponse>() {
            @Override
            public void onResponse(
                    Call<FeedDetailResponse> call,
                    Response<FeedDetailResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("피드 상세 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<FeedDetailResponse> call,
                    Throwable t
            ) {
                callback.onError("서버 연결 실패: " + t.getMessage());
            }
        });
    }

    /*
     * 피드 업로드를 처리하는 함수임
     * 현재 업로드 기능은 미완성 상태임
     * 사진 업로드는 추후 multipart 방식으로 수정될 수 있음
     */
    public void uploadFeed(
            FeedUploadRequest request,
            RepositoryCallback<FeedResponse> callback
    ) {
        feedApi.uploadFeed(request).enqueue(new Callback<FeedResponse>() {
            @Override
            public void onResponse(
                    Call<FeedResponse> call,
                    Response<FeedResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("피드 업로드 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<FeedResponse> call,
                    Throwable t
            ) {
                callback.onError("서버 연결 실패: " + t.getMessage());
            }
        });
    }

    /*
     * 태그할 친구 목록을 조회하는 함수임
     * FeedApi의 친구 목록 API를 호출하고 결과를 화면으로 전달함
     */
    public void getFriendList(
            RepositoryCallback<List<TagUser>> callback
    ) {
        int userId = TokenManager.getUserId(context);

        feedApi.getFriendList(
                (long) userId,
                "ACCEPTED"
        ).enqueue(new Callback<List<TagUser>>() {
            @Override
            public void onResponse(
                    Call<List<TagUser>> call,
                    Response<List<TagUser>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("친구 목록 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<List<TagUser>> call,
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