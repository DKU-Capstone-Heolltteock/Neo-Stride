package com.neostride.app.feature.tip.model;

import java.util.List;

/*
 * 팁 상세 조회 응답 DTO 클래스임
 * GET /api/community/tips/{tipId} 응답 데이터를 저장함
 */
public class TipDetailResponse {

    // 팁 게시글 ID임
    private Long tipId;

    // 작성자 ID임
    private Long writerId;

    // 작성자 닉네임임
    private String nickname;

    // 작성자 프로필 이미지 URL임
    private String profileImageUrl;

    // 작성자가 뱃지를 보유했는지 여부임
    private boolean badgeOwned;

    // 뱃지 종류임
    // 서버 필드명이 다르면 나중에 맞춰야 함
    private String badgeType;

    // 팁 카테고리임
    private String category;

    // 팁 제목임
    private String title;

    // 팁 내용임
    private String content;

    // GPS 기록 공개 여부임
    private boolean gpsVisible;

    // 경로 지도 이미지 URL임
    private String routeMapImageUrl;

    // 첨부 이미지 URL 목록임
    private List<String> imageUrls;

    // 좋아요 수임
    private int likeCount;

    // 댓글 수임
    private int commentCount;

    // 현재 로그인한 사용자가 좋아요를 눌렀는지 여부임
    private boolean liked;

    // 현재 로그인한 사용자가 북마크했는지 여부임
    private boolean bookmarked;

    // 현재 로그인한 사용자가 작성자인지 여부임
    private boolean mine;

    // 작성 시간임
    private String createdAt;

    // 댓글 목록임
    private List<TipCommentResponse> comments;

    public Long getTipId() {
        return tipId;
    }

    public Long getWriterId() {
        return writerId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public boolean isBadgeOwned() {
        return badgeOwned;
    }

    public String getBadgeType() {
        return badgeType;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isGpsVisible() {
        return gpsVisible;
    }

    public String getRouteMapImageUrl() {
        return routeMapImageUrl;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public boolean isLiked() {
        return liked;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public boolean isMine() {
        return mine;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<TipCommentResponse> getComments() {
        return comments;
    }
}