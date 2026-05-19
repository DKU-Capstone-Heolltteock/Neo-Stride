package com.neostride.app.feature.community.mypage.repository;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.community.mypage.api.MyPageService;
import com.neostride.app.feature.community.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.community.mypage.model.UserProfileResponse;
import com.neostride.app.feature.community.tip.model.TipResponse;

import java.util.List;

import retrofit2.Callback;


//  마이페이지 데이터 레포지터리
//  <p>
//  - {@link MyPageService}를 통해 프로필·피드·북마크 API를 호출하고 Callback으로 결과를 전달한다.

public class MyPageRepository {
    private MyPageService apiService;


    public MyPageRepository() {
        this.apiService = ApiClient.getInstance().create(MyPageService.class);
    }
    //아래는 목서버연결
    /*public MyPageRepository() {
        this.apiService = MockApiClient.getInstance().create(MyPageService.class);
    }*/

    // 현재 사용자 프로필 정보를 조회한다.
    public void getUserProfile(Callback<UserProfileResponse> callback) {
        apiService.getUserProfile().enqueue(callback);
    }

    // 내가 쓴 피드 목록을 조회한다.
    public void getMyFeeds(Callback<List<CommunityContentResponse>> callback) {
        apiService.getMyFeeds().enqueue(callback);
    }

    // 내가 쓴 팁 목록을 조회한다.
    public void getMyTips(Callback<List<TipResponse>> callback) {
        apiService.getMyTips().enqueue(callback);
    }

    // 나를 태그한 피드 목록을 조회한다.
    public void getTaggedFeeds(Callback<List<CommunityContentResponse>> callback) {
        apiService.getTaggedFeeds().enqueue(callback);
    }

    // 내가 댓글 단 피드 목록을 조회한다.
    public void getCommentedFeeds(Callback<List<CommunityContentResponse>> callback) {
        apiService.getCommentedFeeds().enqueue(callback);
    }

    // 내가 좋아요 한 피드 목록을 조회한다.
    public void getLikedFeeds(Callback<List<CommunityContentResponse>> callback) {
        apiService.getLikedFeeds().enqueue(callback);
    }

    // 내가 북마크 한 피드 목록을 조회한다.
    public void getBookmarkedFeeds(Callback<List<CommunityContentResponse>> callback) {
        apiService.getBookmarkedFeeds().enqueue(callback);
    }

    // 상태 메시지를 서버에 수정 요청한다.
    public void updateStatusMessage(String message, Callback<Void> callback) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("status_message", message);
        apiService.updateStatusMessage(body).enqueue(callback);
    }

    // 프로필 이미지를 Multipart로 서버에 업로드한다.
    public void updateProfileImage(okhttp3.MultipartBody.Part imagePart, Callback<Void> callback) {
        apiService.updateProfileImage(imagePart).enqueue(callback);
    }

    // 북마크 상태를 토글하여 서버에 저장/해제 요청한다.
    public void toggleBookmark(int contentId, boolean isBookmarked, Callback<Void> callback) {
        // 백엔드 설계에 따라 넘기는 값이 달라질 수 있습니다.
        // 예시: { "is_bookmarked": true }
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("is_bookmarked", isBookmarked);

        // apiService에 toggleBookmark 메서드가 정의되어 있어야 합니다.
        apiService.toggleBookmark(contentId, body).enqueue(callback);
    }
}