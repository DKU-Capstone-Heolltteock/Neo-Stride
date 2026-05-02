package com.neostride.app.feature.feed.model;

// 피드 카드 1개의 데이터를 담는 클래스임
public class FeedItem {

    private String username;      // 사용자 이름
    private String time;          // 작성 시간
    private String title;         // 피드 제목

    private int tagCount;         // 태그 수
    private int likeCount;        // 좋아요 수
    private int commentCount;     // 댓글 수

    private String distance;      // 러닝 거리
    private String duration;      // 러닝 시간
    private String pace;          // 평균 페이스

    private int photoResId;       // 사용자가 올린 사진 이미지
    private int routeMapResId;    // GPS 경로를 이미지처럼 보여주는 지도 이미지

    public FeedItem(String username, String time, String title,
                    int tagCount, int likeCount, int commentCount,
                    String distance, String duration, String pace,
                    int photoResId, int routeMapResId) {

        this.username = username;
        this.time = time;
        this.title = title;
        this.tagCount = tagCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.distance = distance;
        this.duration = duration;
        this.pace = pace;
        this.photoResId = photoResId;
        this.routeMapResId = routeMapResId;
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

    public int getPhotoResId() {
        return photoResId;
    }

    public int getRouteMapResId() {
        return routeMapResId;
    }
}