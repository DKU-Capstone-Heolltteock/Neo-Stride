package com.neostride.app.feature.search.repository;

import android.content.Context;

import com.neostride.app.common.network.ApiClient;
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
 * - 피드/팁/프로필: page(0-indexed) + size 기반 페이지네이션
 * - 친구: 페이지네이션 없이 전체 목록 한 번에 로드
 */
public class SearchRepository {

    private final SearchApi searchApi;
    private final Context context;

    public interface FeedSearchCallback {
        void onSuccess(List<FeedResponse> feedResponses);
        void onFailure(String message);
    }

    public interface TipSearchCallback {
        void onSuccess(List<TipResponse> tipResponses);
        void onFailure(String message);
    }

    public interface UserSearchCallback {
        void onSuccess(List<SearchUserResponse> userResponses);
        void onFailure(String message);
    }

    public SearchRepository(Context context) {
        this.context = context.getApplicationContext();
        this.searchApi = ApiClient.getInstance().create(SearchApi.class);
    }

    /*
     * 피드 검색/최신 목록 페이지 요청임
     * keyword 없으면 최신순, 있으면 title+content 검색
     */
    public void searchFeeds(String keyword, int page, int size, FeedSearchCallback callback) {
        String safeKeyword = getSafeKeyword(keyword);

        searchApi.searchFeeds(safeKeyword, page, size).enqueue(new Callback<List<FeedResponse>>() {
            @Override
            public void onResponse(Call<List<FeedResponse>> call, Response<List<FeedResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<FeedResponse>> call, Throwable t) {
                callback.onFailure("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 팁 검색/최신 목록 페이지 요청임
     * keyword 없으면 최신순, 있으면 title+content 검색
     */
    public void searchTips(String keyword, String category, int page, int size, TipSearchCallback callback) {
        String safeKeyword = getSafeKeyword(keyword);
        String safeCategory = getSafeCategory(category);

        searchApi.searchTips(safeKeyword, safeCategory, page, size).enqueue(new Callback<List<TipResponse>>() {
            @Override
            public void onResponse(Call<List<TipResponse>> call, Response<List<TipResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<TipResponse>> call, Throwable t) {
                callback.onFailure("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 프로필 키워드 검색 페이지 요청임
     */
    public void searchProfiles(String keyword, int page, int size, UserSearchCallback callback) {
        String safeKeyword = getSafeKeyword(keyword);

        searchApi.searchProfiles(safeKeyword, page, size).enqueue(new Callback<List<SearchUserResponse>>() {
            @Override
            public void onResponse(Call<List<SearchUserResponse>> call, Response<List<SearchUserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SearchUserResponse>> call, Throwable t) {
                callback.onFailure("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 친구 키워드 검색 요청임 (페이지네이션 없이 전체 반환)
     */
    public void searchFriends(String keyword, UserSearchCallback callback) {
        String safeKeyword = getSafeKeyword(keyword);

        searchApi.searchFriends(safeKeyword).enqueue(new Callback<List<SearchUserResponse>>() {
            @Override
            public void onResponse(Call<List<SearchUserResponse>> call, Response<List<SearchUserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SearchUserResponse>> call, Throwable t) {
                callback.onFailure("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 배지 등급 기준 상위 프로필 페이지 요청임 (프로필 탭 키워드 없을 때 사용)
     */
    public void getTopProfiles(int page, int size, UserSearchCallback callback) {
        searchApi.getTopProfiles(page, size).enqueue(new Callback<List<SearchUserResponse>>() {
            @Override
            public void onResponse(Call<List<SearchUserResponse>> call, Response<List<SearchUserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SearchUserResponse>> call, Throwable t) {
                callback.onFailure("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 내 친구 전체 목록 요청임 (친구 탭 — 페이지네이션 없이 전체 로드)
     */
    public void getMyFriends(UserSearchCallback callback) {
        searchApi.getMyFriends().enqueue(new Callback<List<SearchUserResponse>>() {
            @Override
            public void onResponse(Call<List<SearchUserResponse>> call, Response<List<SearchUserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SearchUserResponse>> call, Throwable t) {
                callback.onFailure("서버에 연결할 수 없습니다");
            }
        });
    }

    private String getSafeKeyword(String keyword) {
        if (keyword == null) return "";
        return keyword.trim();
    }

    private String getSafeCategory(String category) {
        if (category == null || category.trim().isEmpty()) return "ALL";
        return category.trim().toUpperCase();
    }
}
