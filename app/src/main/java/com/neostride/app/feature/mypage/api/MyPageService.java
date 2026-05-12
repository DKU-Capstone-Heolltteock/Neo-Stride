package com.neostride.app.feature.mypage.api;

import com.neostride.app.feature.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.mypage.model.UserProfileResponse;

import java.util.List;

import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface MyPageService {

    // 1. 프로필 정보 조회
    @GET("users/me/profile")
    Call<UserProfileResponse> getUserProfile();

    // 상태 메시지 수정
    @PATCH("users/me/status")
    Call<Void> updateStatusMessage(@Body Map<String, String> body);

    // 프로필 이미지 업로드
    @Multipart
    @PATCH("users/me/profile-image")
    Call<Void> updateProfileImage(@Part MultipartBody.Part image);

    // 2. 내가 쓴 피드 목록
    @GET("community/contents/me")
    Call<List<CommunityContentResponse>> getMyFeeds();

    // [추가] 3. 나를 태그한 피드 목록
    @GET("community/contents/tagged")
    Call<List<CommunityContentResponse>> getTaggedFeeds();

    // [추가] 4. 내가 댓글 단 피드 목록
    @GET("community/contents/comments")
    Call<List<CommunityContentResponse>> getCommentedFeeds();

    // [추가] 5. 내가 좋아요 한 피드 목록
    @GET("community/contents/likes")
    Call<List<CommunityContentResponse>> getLikedFeeds();

    // [추가] 6. 내가 북마크 한 피드 목록
    @GET("community/contents/bookmarks")
    Call<List<CommunityContentResponse>> getBookmarkedFeeds();

    @POST("community/bookmark/{contentId}")
    Call<Void> toggleBookmark(
            @Path("contentId") int contentId,
            @Body java.util.Map<String, Object> body
    );
}