package com.neostride.app.feature.search.api;

import com.neostride.app.feature.feed.model.FeedResponse;
import com.neostride.app.feature.search.model.SearchUserResponse;
import com.neostride.app.feature.tip.model.TipResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/*
 * 검색 API 인터페이스임
 *
 * 기존처럼 SearchItem 하나로 전부 받지 않고,
 * 피드/팁/프로필/친구를 각각 실제 목록 화면에서 쓰는 모델에 맞게 따로 받음
 *
 * 이렇게 해야 검색 결과에서도 기존 피드 목록 디자인과 팁 목록 디자인을 그대로 재사용할 수 있음
 */
public interface SearchApi {

    /*
     * 피드 검색 API임
     * 검색 결과를 FeedResponse 목록으로 받아 기존 FeedAdapter에 연결할 수 있게 함
     */
    @GET("/api/community/search/feeds")
    Call<List<FeedResponse>> searchFeeds(
            @Query("keyword") String keyword
    );

    /*
     * 팁 검색 API임
     * 검색 결과를 TipResponse 목록으로 받아 기존 TipAdapter에 연결할 수 있게 함
     *
     * category:
     * - ALL
     * - FREE
     * - TRAINING
     * - COURSE
     * - GEAR
     */
    @GET("/api/community/search/tips")
    Call<List<TipResponse>> searchTips(
            @Query("keyword") String keyword,
            @Query("category") String category
    );

    /*
     * 프로필 검색 API임
     * 러너 프로필 검색 결과를 유저 검색 전용 DTO로 받음
     */
    @GET("/api/community/search/profiles")
    Call<List<SearchUserResponse>> searchProfiles(
            @Query("keyword") String keyword
    );

    /*
     * 친구 검색 API임
     * 친구 검색 결과를 유저 검색 전용 DTO로 받음
     */
    @GET("/api/community/search/friends")
    Call<List<SearchUserResponse>> searchFriends(
            @Query("keyword") String keyword
    );
}