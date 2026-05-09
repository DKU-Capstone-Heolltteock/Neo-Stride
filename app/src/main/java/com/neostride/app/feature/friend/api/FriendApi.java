package com.neostride.app.feature.friend.api;

import com.neostride.app.feature.friend.model.FriendResponse;
import com.neostride.app.feature.friend.model.FriendRequest;

import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface FriendApi {
    // 1. 관계별 리스트 조회 (status: friends, sent, received, blocked)
    @GET("api/relationships")
    Call<List<FriendResponse>> getFriendList(@Query("status") String status);

    // 2. 관계 상태 변경 (요청 취소, 수락, 거절, 차단 등)
    @POST("api/relationships/action")
    Call<ResponseBody> updateRelationship(@Body FriendRequest request);
}
