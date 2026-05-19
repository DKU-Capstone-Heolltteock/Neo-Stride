package com.neostride.app.feature.community.tip.model;

import java.util.List;

/*
 * 팁 업로드 요청 DTO 클래스임
 */
public class TipUploadRequest {

    private String category;
    private String title;
    private String content;

    private boolean gpsVisible;

    private String routeMapImageUrl;

    private List<String> imageUrls;

    public TipUploadRequest(
            String category,
            String title,
            String content,
            boolean gpsVisible,
            String routeMapImageUrl,
            List<String> imageUrls
    ) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.gpsVisible = gpsVisible;
        this.routeMapImageUrl = routeMapImageUrl;
        this.imageUrls = imageUrls;
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
}