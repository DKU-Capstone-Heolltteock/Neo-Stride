package com.neostride.app.feature.friend.repository;

import com.neostride.app.feature.friend.api.FriendApi;
import com.neostride.app.feature.friend.model.FriendResponse;
import com.neostride.app.feature.friend.model.FriendRequest;

import java.util.List;
import java.util.function.Consumer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRepository {
    private final FriendApi api;

    public FriendRepository(FriendApi api) {
        this.api = api;
    }

    // 목록 가져오기
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

    // 특정 유저의 친구 목록 조회
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

    // 상태 변경 요청 (취소/수락 등)
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