package com.neostride.app.feature.feed.api;

import com.neostride.app.feature.feed.model.FeedResponse;
import com.neostride.app.feature.feed.model.FeedUploadRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/*
 * 피드 관련 API 인터페이스임
 * Retrofit이 이 인터페이스를 기반으로 실제 HTTP 통신 코드를 자동 생성함
 */
public interface FeedApi {

    /*
     * 피드 업로드 API임
     * POST /feeds 요청을 서버로 전송함
     *
     * @Body
     * 서버로 전송할 피드 업로드 데이터임
     */
    @POST("/feeds")
    Call<FeedResponse> uploadFeed(
            @Body FeedUploadRequest request
    );

    /*
     * 피드 목록 조회 API임
     * GET /feeds 요청을 서버로 전송함
     *
     * @Header("X-User-Id")
     * 현재 로그인한 사용자 ID를 헤더로 전달함
     *
     * 서버는 사용자 정보를 기준으로
     * 좋아요 여부, 북마크 여부 등을 판단 가능함
     */
    @GET("/feeds")
    Call<List<FeedResponse>> getFeedList(
            @Header("X-User-Id") Long userId
    );
}