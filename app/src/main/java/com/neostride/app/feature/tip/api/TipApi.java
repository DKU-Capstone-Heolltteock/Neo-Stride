package com.neostride.app.feature.tip.api;

import com.neostride.app.feature.tip.model.TipDetailResponse;
import com.neostride.app.feature.tip.model.TipResponse;
import com.neostride.app.feature.tip.model.TipUploadRequest;
import com.neostride.app.feature.tip.model.TipUploadResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/*
 * 팁 API 인터페이스임
 * Retrofit이 이 인터페이스를 기반으로 HTTP 요청을 생성함
 */
public interface TipApi {

    /*
     * 팁 업로드 API임
     * POST /api/community/tips 요청을 서버로 전송함
     */
    @POST("api/community/tips")
    Call<TipUploadResponse> uploadTip(
            @Body TipUploadRequest request
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
}