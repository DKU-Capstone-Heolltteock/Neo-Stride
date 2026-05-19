package com.neostride.app.feature.community.feed.api;

import com.neostride.app.feature.community.feed.model.FeedDetailResponse;
import com.neostride.app.feature.community.feed.model.FeedResponse;
import com.neostride.app.feature.community.feed.model.FeedUploadRequest;
import com.neostride.app.feature.community.feed.model.TagUser;
import com.neostride.app.feature.community.feed.model.FeedLikeResponse;
import com.neostride.app.feature.community.feed.model.FeedBookmarkResponse;
import com.neostride.app.feature.community.feed.model.FeedCommentRequest;
import com.neostride.app.feature.community.feed.model.FeedCommentResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

/*
 * 피드 관련 API 인터페이스임
 * Retrofit이 이 인터페이스를 기반으로 실제 HTTP 통신 코드를 자동 생성함
 */
public interface FeedApi {

    /*
     * 피드 업로드 API임
     * POST /api/community/feeds — multipart/form-data 방식으로 전송함
     *
     * @PartMap fields  : 텍스트 필드 (title, content, privacy, mapVisible 등)
     * @Part    images  : 피드 사진 + 경로 지도 이미지 (각 Part의 name 으로 구분)
     */
    @Multipart
    @POST("api/community/feeds")
    Call<FeedResponse> uploadFeed(
            @PartMap Map<String, RequestBody> fields,
            @Part List<MultipartBody.Part> images
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

    /*
     * 피드 좋아요 토글 API임
     * POST /api/community/feeds/{feedId}/likes 요청을 보냄
     */
    @POST("api/community/feeds/{feedId}/likes")
    Call<FeedLikeResponse> toggleFeedLike(
            @Path("feedId") Long feedId
    );

    /*
     * 피드 북마크 토글 API임
     * POST /api/community/feeds/{feedId}/bookmarks 요청을 보냄
     */
    @POST("api/community/feeds/{feedId}/bookmarks")
    Call<FeedBookmarkResponse> toggleFeedBookmark(
            @Path("feedId") Long feedId
    );

    @POST("api/community/feeds/{feedId}/comments")
    Call<FeedCommentResponse> createFeedComment(
            @Path("feedId") Long feedId,
            @Body FeedCommentRequest request
    );

    /*
     * 피드 삭제 API임
     * DELETE /api/community/feeds/{feedId}
     */
    @DELETE("api/community/feeds/{feedId}")
    Call<okhttp3.ResponseBody> deleteFeed(
            @Path("feedId") Long feedId
    );

    /*
     * 피드 수정 API임
     * PUT /api/community/feeds/{feedId}
     */
    @PUT("api/community/feeds/{feedId}")
    Call<FeedResponse> updateFeed(
            @Path("feedId") Long feedId,
            @Body FeedUploadRequest request
    );

    /*
     * 피드 댓글 수정 API임
     * PUT /api/community/feeds/{feedId}/comments/{commentId}
     */
    @PUT("api/community/feeds/{feedId}/comments/{commentId}")
    Call<FeedCommentResponse> updateFeedComment(
            @Path("feedId") Long feedId,
            @Path("commentId") Long commentId,
            @Body FeedCommentRequest request
    );

    /*
     * 피드 댓글 삭제 API임
     * DELETE /api/community/feeds/{feedId}/comments/{commentId}
     */
    @DELETE("api/community/feeds/{feedId}/comments/{commentId}")
    Call<okhttp3.ResponseBody> deleteFeedComment(
            @Path("feedId") Long feedId,
            @Path("commentId") Long commentId
    );

    /*
     * 피드에 태그된 사용자 목록 조회 API임
     * GET /api/community/feeds/{feedId}/tagged-users
     */
    @GET("api/community/feeds/{feedId}/tagged-users")
    Call<List<TagUser>> getTaggedUsers(
            @Path("feedId") Long feedId
    );
}