package com.neostride.app.feature.feed.api;

import com.neostride.app.feature.feed.model.FeedDetailResponse;
import com.neostride.app.feature.feed.model.FeedResponse;
import com.neostride.app.feature.feed.model.FeedUploadRequest;
import com.neostride.app.feature.feed.model.TagUser;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/*
 * 피드 관련 API 인터페이스임
 * Retrofit이 이 인터페이스를 기반으로 실제 HTTP 통신 코드를 자동 생성함
 */
public interface FeedApi {

    /*
     * 피드 업로드 API임
     * POST /api/feeds 요청을 서버로 전송함
     *
     * 현재 업로드 기능은 미완성 상태임
     * 사진 업로드는 multipart 방식으로 추후 수정될 수 있음
     *
     * @Body
     * 서버로 전송할 피드 업로드 데이터임
     */
    @POST("api/community/feeds")
    Call<FeedResponse> uploadFeed(
            @Body FeedUploadRequest request
    );

    /*
     * 피드 목록 조회 API임
     * GET /api/feeds 요청을 서버로 전송함
     *
     * @Header("X-User-Id")
     * 현재 로그인한 사용자 ID를 헤더로 전달함
     *
     * 서버는 사용자 정보를 기준으로
     * 좋아요 여부, 북마크 여부 등을 판단할 수 있음
     */
    @GET("api/community/feeds")
    Call<List<FeedResponse>> getFeedList(
            @Header("X-User-Id") Long userId
    );

    /*
     * 피드 상세 조회 API임
     * GET /api/feeds/{feedId} 요청을 서버로 전송함
     *
     * 목록 조회용 FeedResponse가 아니라
     * 상세 화면 전용 FeedDetailResponse를 반환함
     */
    @GET("api/community/feeds/{feedId}")
    Call<FeedDetailResponse> getFeedDetail(
            @Header("X-User-Id") Long userId,
            @Path("feedId") Long feedId
    );

    /*
     * 태그할 친구 목록 조회 API임
     * GET /community/friends 요청을 서버로 전송함
     *
     * 현재 Swagger 기준으로는 /community/friends 이지만,
     * 추후 팀 API 규칙에 따라 /api/community/friends 로 변경될 수 있음
     *
     * @Header("X-User-Id")
     * 현재 로그인한 사용자 ID를 헤더로 전달함
     *
     * @Query("status")
     * 친구 상태값을 쿼리로 전달함
     * 예: ACCEPTED, FRIEND 등
     */
    @GET("api/community/friends")
    Call<List<TagUser>> getFriendList(
            @Header("X-User-Id") Long userId,
            @Query("status") String status
    );
}