package com.neostride.app.feature.community.feed.model;

import java.util.List;

// 피드 카드 1개의 데이터를 담는 클래스임
public class FeedItem {

    // 서버에서 내려주는 피드 고유 ID임
    // 피드 상세 조회, 수정, 삭제, 좋아요 등에 필요함
    private Long feedId;
    // 작성자 유저 ID임
    private Long writerId;
    private String profileImageUrl;
    private String username;
    private boolean badgeOwned;
    private String badgeType;
    private String time;

    private String title;
    private String content;

    private int tagCount;
    private int likeCount;
    private int commentCount;

    private String distance;
    private String duration;
    private String pace;

    private boolean mapVisible;          // 지도 표시 여부임
    private String routeMapImageUri;     // 실제 기록 지도 캡처 이미지 URI임

    private List<String> imageUrls;

    // 현재 로그인 사용자의 인터랙션 상태 — 카드 하이라이트용
    private boolean liked;
    private boolean bookmarked;
    private boolean commented;
    private boolean tagged;

    // 본인 글 여부 — ··· 메뉴 분기용
    private boolean mine;

    public boolean isLiked() { return liked; }
    public boolean isBookmarked() { return bookmarked; }
    public boolean isCommented() { return commented; }
    public boolean isTagged() { return tagged; }
    public boolean isMine() { return mine; }

    public void setLiked(boolean liked) { this.liked = liked; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setBookmarked(boolean bookmarked) { this.bookmarked = bookmarked; }
    public void setCommented(boolean commented) { this.commented = commented; }
    public void setMine(boolean mine) { this.mine = mine; }

    public FeedItem(
            Long feedId,
            Long writerId,
            String profileImageUrl,
            String username,
            boolean badgeOwned,
            String badgeType,
            String time,
            String title,
            String content,
            int tagCount,
            int likeCount,
            int commentCount,
            String distance,
            String duration,
            String pace,
            boolean mapVisible,
            String routeMapImageUri,
            List<String> imageUrls,
            boolean liked,
            boolean bookmarked,
            boolean commented,
            boolean tagged
    ) {
        this.feedId = feedId;
        this.writerId = writerId;
        this.profileImageUrl = profileImageUrl;
        this.username = username;
        this.badgeOwned = badgeOwned;
        this.badgeType = badgeType;
        this.time = time;
        this.title = title;
        this.content = content;
        this.tagCount = tagCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.distance = distance;
        this.duration = duration;
        this.pace = pace;
        this.mapVisible = mapVisible;
        this.routeMapImageUri = routeMapImageUri;
        this.imageUrls = imageUrls;
        this.liked = liked;
        this.bookmarked = bookmarked;
        this.commented = commented;
        this.tagged = tagged;
    }

    public boolean isBadgeOwned() { return badgeOwned; }
    public String getBadgeType() { return badgeType; }

    public Long getFeedId() {
        return feedId;
    }

    public Long getWriterId() {
        return writerId;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getTime() {
        return time;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getTagCount() {
        return tagCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public String getDistance() {
        return distance;
    }

    public String getDuration() {
        return duration;
    }

    public String getPace() {
        return pace;
    }

    public boolean isMapVisible() {
        return mapVisible;
    }

    public String getRouteMapImageUri() {
        return routeMapImageUri;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }
}