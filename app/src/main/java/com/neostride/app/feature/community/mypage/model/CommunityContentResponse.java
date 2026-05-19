package com.neostride.app.feature.community.mypage.model;

import com.google.gson.annotations.SerializedName;


//  커뮤니티 피드 항목 응답 DTO
//  <p>
//  - 피드 내용(제목·본문·이미지), 러닝 통계(거리·시간·페이스),
//    사용자 상호작용(좋아요·댓글·태그·북마크) 정보를 포함한다.

public class CommunityContentResponse {
    @SerializedName("content_id") public int contentId;
    @SerializedName("user_id") public int userId;
    @SerializedName("profile_image_url") public String profileImageUrl; // 프로필 이미지 필드 추가
    @SerializedName("content_title") public String contentTitle;
    @SerializedName("content_text") public String contentText;
    @SerializedName("nickname") public String nickname;
    @SerializedName("total_distance") public double totalDistance;
    @SerializedName("duration") public int duration;
    @SerializedName("pace") public int pace;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("image_url") public String imageUrl;      // 피드 중앙에 들어갈 이미지 경로
    @SerializedName("tag_count") public int tagCount;         // 사람 아이콘 옆 숫자
    @SerializedName("like_count") public int likeCount;       // 하트 아이콘 옆 숫자
    @SerializedName("comment_count") public int commentCount; // 말풍선 아이콘 옆 숫자

    @SerializedName("is_bookmarked") public boolean isBookmarked = false;
    @SerializedName("is_liked") public boolean isLiked = false;
    @SerializedName("is_commented") public boolean isCommented = false;
    @SerializedName("is_tagged") public boolean isTagged = false;
    @SerializedName("badge_tier") public String badgeTier;       // 작성자 배지 티어 (NONE/BRONZE/SILVER/...)
    @SerializedName("route_map_url") public String routeMapUrl;   // 경로 지도 이미지 URL (공개 시 표시)
}