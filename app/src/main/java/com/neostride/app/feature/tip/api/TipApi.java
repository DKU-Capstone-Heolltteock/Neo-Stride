package com.neostride.app.feature.tip.api;

import com.neostride.app.feature.tip.model.TipDetailResponse;
import com.neostride.app.feature.tip.model.TipResponse;
import com.neostride.app.feature.tip.model.TipUploadRequest;
import com.neostride.app.feature.tip.model.TipUploadResponse;
import com.neostride.app.feature.tip.model.TipBookmarkResponse;
import com.neostride.app.feature.tip.model.TipCommentRequest;
import com.neostride.app.feature.tip.model.TipCommentResponse;
import com.neostride.app.feature.tip.model.TipLikeResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;


/*
 * 팁 API 인터페이스임
 * Retrofit이 이 인터페이스를 기반으로 HTTP 요청을 생성함
 */
public interface TipApi {

    /*
     * 팁 업로드 API임
     * POST /api/community/tips — multipart/form-data 방식으로 전송함
     *
     * @PartMap fields : 텍스트 필드 (category, title, content, gpsVisible 등)
     * @Part    images : 첨부 사진 + GPS 경로 지도 이미지
     */
    @Multipart
    @POST("api/community/tips")
    Call<TipUploadResponse> uploadTip(
            @PartMap Map<String, RequestBody> fields,
            @Part List<MultipartBody.Part> images
    );

    /*
     * 팁 목록 조회 API임
     * GET /api/community/tips 요청을 서버로 전송함
     * 피드 목록과 동일하게 배열 형태로 응답을 받음
     */
    @GET("api/community/tips")
    Call<List<TipResponse>> getTips();

    /*
     * 팁 상세 조회 API임
     * GET /api/community/tips/{tipId} 요청을 서버로 전송함
     */
    @GET("api/community/tips/{tipId}")
    Call<TipDetailResponse> getTipDetail(
            @Path("tipId") Long tipId
    );

    /*
     * 팁 좋아요 토글 API임
     * POST /api/community/tips/{tipId}/likes 요청을 서버로 전송함
     */
    @POST("api/community/tips/{tipId}/likes")
    Call<TipLikeResponse> toggleTipLike(
            @Path("tipId") Long tipId
    );

    /*
     * 팁 북마크 토글 API임
     * POST /api/community/tips/{tipId}/bookmarks 요청을 서버로 전송함
     */
    @POST("api/community/tips/{tipId}/bookmarks")
    Call<TipBookmarkResponse> toggleTipBookmark(
            @Path("tipId") Long tipId
    );

    /*
     * 팁 댓글 작성 API임
     * POST /api/community/tips/{tipId}/comments 요청을 서버로 전송함
     */
    @POST("api/community/tips/{tipId}/comments")
    Call<TipCommentResponse> createTipComment(
            @Path("tipId") Long tipId,
            @Body TipCommentRequest request
    );

    /*
     * 팁 삭제 API임
     * DELETE /api/community/tips/{tipId}
     */
    @DELETE("api/community/tips/{tipId}")
    Call<okhttp3.ResponseBody> deleteTip(
            @Path("tipId") Long tipId
    );

    /*
     * 팁 수정 API임
     * PUT /api/community/tips/{tipId}
     */
    @PUT("api/community/tips/{tipId}")
    Call<TipUploadResponse> updateTip(
            @Path("tipId") Long tipId,
            @Body TipUploadRequest request
    );

    /*
     * 팁 댓글 수정 API임
     * PUT /api/community/tips/{tipId}/comments/{commentId}
     */
    @PUT("api/community/tips/{tipId}/comments/{commentId}")
    Call<TipCommentResponse> updateTipComment(
            @Path("tipId") Long tipId,
            @Path("commentId") Long commentId,
            @Body TipCommentRequest request
    );

    /*
     * 팁 댓글 삭제 API임
     * DELETE /api/community/tips/{tipId}/comments/{commentId}
     */
    @DELETE("api/community/tips/{tipId}/comments/{commentId}")
    Call<okhttp3.ResponseBody> deleteTipComment(
            @Path("tipId") Long tipId,
            @Path("commentId") Long commentId
    );

}