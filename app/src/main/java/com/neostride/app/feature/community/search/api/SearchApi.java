package com.neostride.app.feature.community.search.api;

import com.neostride.app.feature.community.feed.model.FeedResponse;
import com.neostride.app.feature.community.search.model.SearchUserResponse;
import com.neostride.app.feature.community.tip.model.TipResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/*
 * 검색 API 인터페이스임
 *
 * 피드/팁/프로필: page(0-indexed) + size(=10) 기반 페이지네이션
 * - 탭 진입 또는 keyword 변경 시 page=0부터 시작
 * - 스크롤 끝 도달 시 page 증가하며 append
 * 친구 탭: 페이지네이션 없이 전체 목록 한 번에 로드
 */
public interface SearchApi {

    /*
     * 피드 검색/최신 목록 API임
     * keyword 없으면 최신순, 있으면 title+content 검색
     * page: 0-indexed 페이지 번호, size: 페이지당 항목 수
     */
    @GET("/api/community/search/feeds")
    Call<List<FeedResponse>> searchFeeds(
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("size") int size
    );

    /*
     * 팁 검색/최신 목록 API임
     * keyword 없으면 최신순, 있으면 title+content 검색
     * category: ALL / FREE / TRAINING / COURSE / GEAR
     * page: 0-indexed 페이지 번호, size: 페이지당 항목 수
     */
    @GET("/api/community/search/tips")
    Call<List<TipResponse>> searchTips(
            @Query("keyword") String keyword,
            @Query("category") String category,
            @Query("page") int page,
            @Query("size") int size
    );

    /*
     * 키워드로 프로필 검색하는 API임
     * page: 0-indexed 페이지 번호, size: 페이지당 항목 수
     */
    @GET("/api/community/search/profiles")
    Call<List<SearchUserResponse>> searchProfiles(
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("size") int size
    );

    /*
     * 키워드로 친구 검색하는 API임
     * 친구는 페이지네이션 없이 전체 반환함
     */
    @GET("/api/community/search/friends")
    Call<List<SearchUserResponse>> searchFriends(
            @Query("keyword") String keyword
    );

    /*
     * 배지 등급 기준 상위 프로필을 가져오는 API임
     * 프로필 탭 초기 진입 시 사용함
     * page: 0-indexed 페이지 번호, size: 페이지당 항목 수
     */
    @GET("/api/community/search/top-profiles")
    Call<List<SearchUserResponse>> getTopProfiles(
            @Query("page") int page,
            @Query("size") int size
    );

    /*
     * 내 친구 전체 목록을 가져오는 API임
     * 친구 탭은 페이지네이션 없이 전체 로드함
     */
    @GET("/api/community/search/my-friends")
    Call<List<SearchUserResponse>> getMyFriends();
}
