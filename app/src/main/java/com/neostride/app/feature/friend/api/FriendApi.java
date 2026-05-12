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
    /**
     * 1. 관계별 리스트 조회
     * Mockserver의 uri.contains("community/friends") 조건과 일치하도록 경로를 수정했습니다.
     * @param status: friends, sent, received, blocked
     */
    @GET("community/friends")
    Call<List<FriendResponse>> getFriendList(@Query("status") String status);

    /**
     * 2. 관계 상태 변경 (요청 취소, 수락, 거절, 차단 등)
     * 주소 체계의 일관성을 위해 community/friends/action으로 변경했습니다.
     */
    @POST("community/friends/action")
    Call<ResponseBody> updateRelationship(@Body FriendRequest request);

    /**
     * 3. 특정 유저의 친구 목록 조회 (친구 관계인 경우에만 접근 가능)
     */
    @GET("community/friends/user/{userId}")
    Call<List<FriendResponse>> getUserFriendList(@retrofit2.http.Path("userId") int userId);
}