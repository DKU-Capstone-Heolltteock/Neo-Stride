package com.neostride.app.feature.feed.api;

import com.neostride.app.feature.feed.model.FeedUploadRequest;
import com.neostride.app.feature.feed.model.FeedUploadResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface FeedApi {

    // 피드 업로드
    @POST("/feeds")
    Call<FeedUploadResponse> uploadFeed(
            @Body FeedUploadRequest request
    );

    // 피드 목록 조회
    @GET("/feeds")
    Call<List<FeedUploadResponse>> getFeedList();
}