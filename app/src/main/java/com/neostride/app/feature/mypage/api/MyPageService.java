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

//  마이페이지 API 인터페이스
//  <p>
//  - 프로필 조회·수정, 피드 목록(내 피드/태그/댓글/좋아요/북마크), 북마크 토글 엔드포인트를 정의한다.

public interface MyPageService {

    // 현재 로그인 사용자의 프로필 정보 조회
    @GET("users/me/profile")
    Call<UserProfileResponse> getUserProfile();

    // 상태 메시지 수정
    @PATCH("users/me/status")
    Call<Void> updateStatusMessage(@Body Map<String, String> body);

    // 프로필 이미지 Multipart 업로드
    @Multipart
    @PATCH("users/me/profile-image")
    Call<Void> updateProfileImage(@Part MultipartBody.Part image);

    // 내가 쓴 피드 목록 조회
    @GET("community/contents/me")
    Call<List<CommunityContentResponse>> getMyFeeds();

    // 나를 태그한 피드 목록 조회
    @GET("community/contents/tagged")
    Call<List<CommunityContentResponse>> getTaggedFeeds();

    // 내가 댓글 단 피드 목록 조회
    @GET("community/contents/comments")
    Call<List<CommunityContentResponse>> getCommentedFeeds();

    // 내가 좋아요 한 피드 목록 조회
    @GET("community/contents/likes")
    Call<List<CommunityContentResponse>> getLikedFeeds();

    // 내가 북마크 한 피드 목록 조회
    @GET("community/contents/bookmarks")
    Call<List<CommunityContentResponse>> getBookmarkedFeeds();

    // 북마크 상태 토글 (저장/해제)
    @POST("community/bookmark/{contentId}")
    Call<Void> toggleBookmark(
            @Path("contentId") int contentId,
            @Body java.util.Map<String, Object> body
    );
}