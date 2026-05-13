package com.neostride.app.feature.friend.repository;

import com.neostride.app.feature.friend.api.FriendApi;
import com.neostride.app.feature.friend.model.FriendResponse;
import com.neostride.app.feature.friend.model.FriendRequest;

import java.util.List;
import java.util.function.Consumer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


//  친구 관계 데이터 레포지터리
//  <p>
//  - {@link FriendApi}를 통해 친구 목록 조회 및 관계 상태 변경 API를 호출한다.

public class FriendRepository {
    private final FriendApi api;

    public FriendRepository(FriendApi api) {
        this.api = api;
    }

    // 상태별 친구 목록을 서버에서 조회하여 콜백으로 전달한다.
    public void fetchFriendList(String status, Consumer<List<FriendResponse>> callback) {
        api.getFriendList(status).enqueue(new Callback<List<FriendResponse>>() {
            @Override
            public void onResponse(Call<List<FriendResponse>> call, Response<List<FriendResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.accept(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<FriendResponse>> call, Throwable t) {
                // 에러 처리 로직 추가 가능
            }
        });
    }

    // 특정 사용자의 친구 목록을 서버에서 조회한다. (친구 관계인 경우에만 접근 가능)
    public void fetchUserFriendList(int userId, Consumer<List<FriendResponse>> callback) {
        api.getUserFriendList(userId).enqueue(new Callback<List<FriendResponse>>() {
            @Override
            public void onResponse(Call<List<FriendResponse>> call, Response<List<FriendResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.accept(response.body());
                } else {
                    callback.accept(null);
                }
            }
            @Override
            public void onFailure(Call<List<FriendResponse>> call, Throwable t) {
                callback.accept(null);
            }
        });
    }

    // 친구 관계 상태 변경 요청을 서버로 전송하고 성공 여부를 콜백으로 전달한다.
    public void updateStatus(FriendRequest request, Consumer<Boolean> callback) {
        api.updateRelationship(request).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                callback.accept(response.isSuccessful());
            }
            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                callback.accept(false);
            }
        });
    }
}