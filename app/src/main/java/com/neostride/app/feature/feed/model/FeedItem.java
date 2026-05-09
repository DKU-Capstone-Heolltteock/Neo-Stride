package com.neostride.app.feature.feed.model;

import java.util.List;

// 피드 카드 1개의 데이터를 담는 클래스임
public class FeedItem {

    private String profileImageUrl;
    private String username;
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

    public FeedItem(
            String profileImageUrl,
            String username,
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
            List<String> imageUrls
    ) {
        this.profileImageUrl = profileImageUrl;
        this.username = username;
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