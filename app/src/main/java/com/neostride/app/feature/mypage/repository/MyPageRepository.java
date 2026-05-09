package com.neostride.app.feature.mypage.repository;

import android.util.Log;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.mypage.api.MyPageService;
import com.neostride.app.feature.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.mypage.model.UserProfileResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class MyPageRepository {
    private MyPageService apiService;

    public MyPageRepository() {
        this.apiService = ApiClient.getInstance().create(MyPageService.class);
    }

    // 1. 프로필 정보 조회
    public void getUserProfile(Callback<UserProfileResponse> callback) {
        apiService.getUserProfile().enqueue(callback);
    }

    // 2. 내가 쓴 피드 목록 조회
    public void getMyFeeds(Callback<List<CommunityContentResponse>> callback) {
        apiService.getMyFeeds().enqueue(callback);
    }

    // [추가] 3. 나를 태그한 피드 목록 조회
    public void getTaggedFeeds(Callback<List<CommunityContentResponse>> callback) {
        apiService.getTaggedFeeds().enqueue(callback);
    }

    // [추가] 4. 내가 댓글 단 피드 목록 조회
    public void getCommentedFeeds(Callback<List<CommunityContentResponse>> callback) {
        apiService.getCommentedFeeds().enqueue(callback);
    }

    // [추가] 5. 내가 좋아요 한 피드 목록 조회
    public void getLikedFeeds(Callback<List<CommunityContentResponse>> callback) {
        apiService.getLikedFeeds().enqueue(callback);
    }

    // [추가] 6. 내가 북마크 한 피드 목록 조회
    public void getBookmarkedFeeds(Callback<List<CommunityContentResponse>> callback) {
        apiService.getBookmarkedFeeds().enqueue(callback);
    }

    // 상태 메시지 수정
    public void updateStatusMessage(String message, Callback<Void> callback) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("status_message", message);
        apiService.updateStatusMessage(body).enqueue(callback);
    }

    // 프로필 이미지 업로드
    public void updateProfileImage(okhttp3.MultipartBody.Part imagePart, Callback<Void> callback) {
        apiService.updateProfileImage(imagePart).enqueue(callback);
    }
}