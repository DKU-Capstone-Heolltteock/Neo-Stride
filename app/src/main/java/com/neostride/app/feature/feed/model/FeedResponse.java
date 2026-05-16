package com.neostride.app.feature.feed.model;

import java.util.List;

/*
 * 피드 응답 DTO 클래스임
 * 서버에서 피드 목록 조회, 피드 상세 조회, 피드 작성, 피드 수정 후 반환하는 데이터를 저장함
 *
 * Swagger 응답 JSON 구조와 필드명을 동일하게 맞춤
 * Retrofit + Gson이 JSON key와 Java 변수명을 기준으로 자동 매핑함
 */
public class FeedResponse {

    // 피드 고유 ID임
    private Long feedId;

    // 작성자 프로필 이미지 URL임
    private String profileImageUrl;

    // 작성자 닉네임임
    private String nickname;

    // 피드 생성 시간임
    private String createdAt;

    // 피드 제목임
    private String title;

    // 피드 내용임
    private String content;

    // 태그된 사용자 수임
    private int taggedCount;

    // 좋아요 수임
    private int likeCount;

    // 댓글 수임
    private int commentCount;

    // 러닝 거리임
    // Swagger에서 string 타입으로 정의되어 있으므로 String으로 받음
    private String distance;

    // 러닝 시간임
    // Swagger에서는 duration 필드명으로 내려옴
    private String duration;

    // 평균 페이스임
    private String pace;

    // 지도 표시 여부임
    private boolean mapVisible;

    // 경로 지도 이미지 URI임
    private String routeMapImageUri;

    // 피드에 첨부된 이미지 URL 목록임
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