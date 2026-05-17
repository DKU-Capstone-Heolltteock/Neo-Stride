package com.neostride.app.feature.tip.model;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/*
 * 팁 게시글 데이터를 화면에 표시하기 위한 모델 클래스임
 * 서버 응답 DTO를 그대로 화면에 쓰지 않고, 화면에 필요한 형태로 변환해서 사용함
 */
public class TipItem {

    // 팁 게시글 ID임
    // 상세 페이지 이동 및 상세 API 호출에 사용함
    private Long tipId;

    // 작성자 유저 ID임
    private Long writerId;

    // 작성자 닉네임임
    private String nickname;

    // 작성자 프로필 이미지 URL임
    private String profileImageUrl;

    // 팁 카테고리임
    private String category;

    // 팁 제목임
    private String title;

    // 팁 내용임
    private String content;

    // 작성자가 배지를 보유했는지 여부임
    private boolean badgeOwner;

    // GPS 기록 공개 여부임
    private boolean gpsVisible;

    // 로컬에서 선택한 이미지 URI 목록임
    // 업로드 직후 임시 표시 등에 사용할 수 있음
    private ArrayList<Uri> imageUris;

    // 서버에서 받은 이미지 URL 목록임
    private List<String> imageUrls;

    // 서버에서 받은 경로 지도 이미지 URL임
    private String routeMapImageUrl;

    // 좋아요 수임
    private int likeCount;

    // 댓글 수임
    private int commentCount;

    // 작성 시간임
    private String createdAt;

    /*
     * 기존 로컬 데이터용 생성자임
     * 업로드 직후 임시 데이터나 더미 데이터 생성 시 사용할 수 있음
     */
    public TipItem(
            String nickname,
            Long writerId,
            String category,
            String title,
            String content,
            boolean badgeOwner,
            boolean gpsVisible,
            ArrayList<Uri> imageUris,
            int likeCount,
            int commentCount
    ) {
        this.tipId = null;
        this.writerId = writerId;
        this.nickname = nickname;
        this.profileImageUrl = null;
        this.category = category;
        this.title = title;
        this.content = content;
        this.badgeOwner = badgeOwner;
        this.gpsVisible = gpsVisible;
        this.imageUris = imageUris;
        this.imageUrls = null;
        this.routeMapImageUrl = null;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdAt = null;
    }

    /*
     * 서버 응답 데이터를 화면용 TipItem으로 만들 때 사용하는 생성자임
     */
    public TipItem(
            Long tipId,
            Long writerId,
            String nickname,
            String profileImageUrl,
            String category,
            String title,
            String content,
            boolean badgeOwner,
            boolean gpsVisible,
            List<String> imageUrls,
            String routeMapImageUrl,
            int likeCount,
            int commentCount,
            String createdAt
    ) {
        this.tipId = tipId;
        this.writerId = writerId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.category = category;
        this.title = title;
        this.content = content;
        this.badgeOwner = badgeOwner;
        this.gpsVisible = gpsVisible;
        this.imageUris = new ArrayList<>();
        this.imageUrls = imageUrls;
        this.routeMapImageUrl = routeMapImageUrl;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
    }

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

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isBadgeOwner() {
        return badgeOwner;
    }

    public boolean isGpsVisible() {
        return gpsVisible;
    }

    public ArrayList<Uri> getImageUris() {
        return imageUris;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public String getRouteMapImageUrl() {
        return routeMapImageUrl;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}