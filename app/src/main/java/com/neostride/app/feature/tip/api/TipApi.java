package com.neostride.app.feature.tip.api;

import com.neostride.app.feature.tip.model.TipListResponse;
import com.neostride.app.feature.tip.model.TipUploadRequest;
import com.neostride.app.feature.tip.model.TipUploadResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/*
 * 팁 API 인터페이스임
 */
public interface TipApi {

    /*
     * 팁 업로드 API
     */
    @POST("/api/tips")
    Call<TipUploadResponse> uploadTip(
            @Body TipUploadRequest request
    );

    /*
     * 팁 목록 조회 API
     */
    @GET("/api/tips")
    Call<TipListResponse> getTips();
}