package com.neostride.app.feature.tip.model;

import java.util.List;

/*
 * 팁 목록 조회 응답 DTO 클래스임
 * GET /api/community/tips 응답 배열 안의 팁 게시글 1개를 저장함
 */
public class TipResponse {

    private Long tipId;

    private String nickname;
    private String profileImageUrl;

    private boolean badgeOwned;
    private String badgeType;

    private String category;

    private String title;
    private String content;

    private boolean gpsVisible;

    private String routeMapImageUrl;

    private List<String> imageUrls;

    private int likeCount;
    private int commentCount;

    private boolean liked;
    private boolean bookmarked;

    private String createdAt;

    public Long getTipId() {
        return tipId;
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

    public String getCreatedAt() {
        return createdAt;
    }
}