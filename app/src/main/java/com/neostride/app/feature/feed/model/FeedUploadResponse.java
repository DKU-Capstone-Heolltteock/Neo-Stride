package com.neostride.app.feature.feed.model;

import java.util.List;
import java.util.Locale;

// 피드 업로드 응답 DTO 클래스임
public class FeedUploadResponse {

    private Long feedId;

    private String profileImageUrl;
    private String nickname;
    private String createdAt;

    private String title;
    private String content;

    private int taggedCount;
    private int likeCount;
    private int commentCount;

    private String distance;
    private String duration;
    private String pace;

    private boolean mapVisible;
    private String routeMapImageUri;

    private List<String> imageUrls;

    public Long getFeedId() {
        return feedId;
    }

    public void setFeedId(Long feedId) {
        this.feedId = feedId;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getTaggedCount() {
        return taggedCount;
    }

    public void setTaggedCount(int taggedCount) {
        this.taggedCount = taggedCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public void setDistance(double distance) {
        this.distance = String.format(
                Locale.KOREA,
                "%.2f km",
                distance
        );
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getPace() {
        return pace;
    }

    public void setPace(String pace) {
        this.pace = pace;
    }

    public boolean isMapVisible() {
        return mapVisible;
    }

    public void setMapVisible(boolean mapVisible) {
        this.mapVisible = mapVisible;
    }

    public String getRouteMapImageUri() {
        return routeMapImageUri;
    }

    public void setRouteMapImageUri(String routeMapImageUri) {
        this.routeMapImageUri = routeMapImageUri;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}