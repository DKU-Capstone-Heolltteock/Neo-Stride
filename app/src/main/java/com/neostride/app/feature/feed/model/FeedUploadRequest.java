package com.neostride.app.feature.feed.model;

import java.util.List;

/*
 * 피드 업로드 요청 DTO 클래스임
 * 앱에서 서버로 POST /feeds 요청을 보낼 때 사용하는 데이터 구조임
 * Swagger의 Request body 필드명과 동일하게 맞춤
 */
public class FeedUploadRequest {

    private String title;
    private String content;
    private String privacy;

    private boolean mapVisible;
    private String routeMapImageUri;

    private List<Long> taggedUserIds;
    private List<String> imageUrls;

    private double distance;
    private String runningTime;
    private String pace;

    private int tagCount;

    public FeedUploadRequest(
            String title,
            String content,
            String privacy,
            boolean mapVisible,
            String routeMapImageUri,
            List<Long> taggedUserIds,
            List<String> imageUrls,
            double distance,
            String runningTime,
            String pace,
            int tagCount
    ) {
        this.title = title;
        this.content = content;
        this.privacy = privacy;
        this.mapVisible = mapVisible;
        this.routeMapImageUri = routeMapImageUri;
        this.taggedUserIds = taggedUserIds;
        this.imageUrls = imageUrls;
        this.distance = distance;
        this.runningTime = runningTime;
        this.pace = pace;
        this.tagCount = tagCount;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getPrivacy() {
        return privacy;
    }

    public boolean isMapVisible() {
        return mapVisible;
    }

    public String getRouteMapImageUri() {
        return routeMapImageUri;
    }

    public List<Long> getTaggedUserIds() {
        return taggedUserIds;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public double getDistance() {
        return distance;
    }

    public String getRunningTime() {
        return runningTime;
    }

    public String getPace() {
        return pace;
    }

    public int getTagCount() {
        return tagCount;
    }
}