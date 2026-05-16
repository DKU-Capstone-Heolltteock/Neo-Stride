package com.neostride.app.feature.search.repository;

import android.content.Context;

import com.neostride.app.common.network.MockApiClient;
import com.neostride.app.feature.feed.model.FeedResponse;
import com.neostride.app.feature.search.api.SearchApi;
import com.neostride.app.feature.search.model.SearchUserResponse;
import com.neostride.app.feature.tip.model.TipResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 * 검색 데이터 처리를 담당하는 Repository 클래스임
 *
 * 정석 구조로 피드/팁/프로필/친구 검색을 분리함
 * 피드 검색은 FeedResponse,
 * 팁 검색은 TipResponse,
 * 프로필/친구 검색은 SearchUserResponse로 받음
 */
public class SearchRepository {

    /*
     * 검색 API 요청을 보내기 위한 Retrofit 인터페이스 객체임
     */
    private final SearchApi searchApi;

    /*
     * Context가 필요할 수 있어 저장함
     */
    private final Context context;

    /*
     * 피드 검색 콜백 인터페이스임
     */
    public interface FeedSearchCallback {
        void onSuccess(List<FeedResponse> feedResponses);
        void onFailure(String message);
    }

    /*
     * 팁 검색 콜백 인터페이스임
     */
    public interface TipSearchCallback {
        void onSuccess(List<TipResponse> tipResponses);
        void onFailure(String message);
    }

    /*
     * 사용자 검색 콜백 인터페이스임
     * 프로필/친구 검색에서 사용함
     */
    public interface UserSearchCallback {
        void onSuccess(List<SearchUserResponse> userResponses);
        void onFailure(String message);
    }

    /*
     * SearchRepository 생성자임
     * 현재는 서버 미완성 상태를 고려해 MockApiClient를 사용함
     */
    public SearchRepository(Context context) {
        this.context = context.getApplicationContext();
        this.searchApi = MockApiClient.getInstance().create(SearchApi.class);
    }

    /*
     * 피드 검색 요청을 실행하는 함수임
     */
    public void searchFeeds(
            String keyword,
            FeedSearchCallback callback
    ) {
        String safeKeyword = getSafeKeyword(keyword);

        searchApi.searchFeeds(safeKeyword).enqueue(new Callback<List<FeedResponse>>() {
            @Override
            public void onResponse(
                    Call<List<FeedResponse>> call,
                    Response<List<FeedResponse>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("피드 검색 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<List<FeedResponse>> call,
                    Throwable t
            ) {
                callback.onFailure("피드 검색 요청 실패: " + t.getMessage());
            }
        });
    }

    /*
     * 팁 검색 요청을 실행하는 함수임
     */
    public void searchTips(
            String keyword,
            String category,
            TipSearchCallback callback
    ) {
        String safeKeyword = getSafeKeyword(keyword);
        String safeCategory = getSafeCategory(category);

        searchApi.searchTips(safeKeyword, safeCategory).enqueue(new Callback<List<TipResponse>>() {
            @Override
            public void onResponse(
                    Call<List<TipResponse>> call,
                    Response<List<TipResponse>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("팁 검색 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<List<TipResponse>> call,
                    Throwable t
            ) {
                callback.onFailure("팁 검색 요청 실패: " + t.getMessage());
            }
        });
    }

    /*
     * 프로필 검색 요청을 실행하는 함수임
     */
    public void searchProfiles(
            String keyword,
            UserSearchCallback callback
    ) {
        String safeKeyword = getSafeKeyword(keyword);

        searchApi.searchProfiles(safeKeyword).enqueue(new Callback<List<SearchUserResponse>>() {
            @Override
            public void onResponse(
                    Call<List<SearchUserResponse>> call,
                    Response<List<SearchUserResponse>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("프로필 검색 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<List<SearchUserResponse>> call,
                    Throwable t
            ) {
                callback.onFailure("프로필 검색 요청 실패: " + t.getMessage());
            }
        });
    }

    /*
     * 친구 검색 요청을 실행하는 함수임
     */
    public void searchFriends(
            String keyword,
            UserSearchCallback callback
    ) {
        String safeKeyword = getSafeKeyword(keyword);

        searchApi.searchFriends(safeKeyword).enqueue(new Callback<List<SearchUserResponse>>() {
            @Override
            public void onResponse(
                    Call<List<SearchUserResponse>> call,
                    Response<List<SearchUserResponse>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("친구 검색 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<List<SearchUserResponse>> call,
                    Throwable t
            ) {
                callback.onFailure("친구 검색 요청 실패: " + t.getMessage());
            }
        });
    }

    /*
     * 검색어 null 처리를 담당하는 함수임
     */
    private String getSafeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }

        return keyword.trim();
    }

    /*
     * 팁 카테고리 null 처리를 담당하는 함수임
     */
    private String getSafeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "ALL";
        }

        return category.trim().toUpperCase();
    }
}