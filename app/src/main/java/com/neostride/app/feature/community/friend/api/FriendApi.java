package com.neostride.app.feature.community.friend.api;

import com.neostride.app.feature.community.friend.model.FriendResponse;
import com.neostride.app.feature.community.friend.model.FriendRequest;

import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;


//  친구 관계 API 인터페이스
//  <p>
//  - 관계별 목록 조회, 관계 상태 변경, 특정 사용자 친구 목록 조회 엔드포인트를 정의한다.

public interface FriendApi {

    // 관계별 친구 목록 조회
    // @param status 조회할 관계 상태 (friends | sent | received | blocked)
    @GET("community/friends")
    Call<List<FriendResponse>> getFriendList(@Query("status") String status);


     // 친구 관계 상태 변경 (요청 취소, 수락, 거절, 차단 등)
    @POST("community/friends/action")
    Call<ResponseBody> updateRelationship(@Body FriendRequest request);


     // 특정 사용자의 친구 목록 조회 (친구 관계인 경우에만 접근 가능)
    @GET("community/friends/user/{userId}")
    Call<List<FriendResponse>> getUserFriendList(@retrofit2.http.Path("userId") int userId);
}