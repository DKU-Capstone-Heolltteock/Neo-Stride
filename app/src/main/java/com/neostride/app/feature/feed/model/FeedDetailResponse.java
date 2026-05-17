package com.neostride.app.feature.feed.model;

import java.util.List;

/*
 * 피드 상세 조회 응답 DTO 클래스임
 * GET /feeds/{feedId} 요청에 대한 상세 피드 데이터를 저장함
 * 목록 조회용 FeedResponse보다 상세 화면에서 필요한 데이터를 더 많이 포함함
 */
public class FeedDetailResponse {

    // 피드 고유 ID임
    private Long feedId;

    // 작성자 고유 ID임
    // 수정/삭제 권한 확인 등에 사용할 수 있음
    private Long writerId;

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

    // 현재 로그인한 사용자가 좋아요를 눌렀는지 여부임
    private boolean liked;

    // 현재 로그인한 사용자가 북마크를 눌렀는지 여부임
    private boolean bookmarked;

    // 현재 로그인한 사용자가 이 피드의 작성자인지 여부임
    private boolean mine;

    // 러닝 거리임
    private String distance;

    // 러닝 시간임
    private String duration;

    // 러닝 페이스임
    private String pace;

    // 지도 표시 여부임
    private boolean mapVisible;

    // 지도 캡처 이미지 URI 또는 URL임
    private String routeMapImageUri;

    // 피드 이미지 URL 목록임
    private List<String> imageUrls;

    // 상세 화면 댓글 목록임
    private List<FeedCommentResponse> comments;

    // 기본 생성자임
    public FeedDetailResponse() {
    }

    public Long getFeedId() {
        return feedId;
    }

    public Long getWriterId() {
        return writerId;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getNickname() {
        return nickname;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getTaggedCount() {
        return taggedCount;
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

    public List<FeedCommentResponse> getComments() {
        return comments;
    }
}