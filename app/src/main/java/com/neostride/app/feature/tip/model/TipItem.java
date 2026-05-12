package com.neostride.app.feature.tip.model;

import android.net.Uri;

import java.util.ArrayList;

/*
 * 팁 게시글 데이터를 저장하는 모델 클래스임
 */
public class TipItem {

    private String nickname;
    private String category;
    private String title;
    private String content;
    private boolean badgeOwner;
    private boolean gpsVisible;
    private ArrayList<Uri> imageUris;
    private int likeCount;
    private int commentCount;

    public TipItem(
            String nickname,
            String category,
            String title,
            String content,
            boolean badgeOwner,
            boolean gpsVisible,
            ArrayList<Uri> imageUris,
            int likeCount,
            int commentCount
    ) {
        this.nickname = nickname;
        this.category = category;
        this.title = title;
        this.content = content;
        this.badgeOwner = badgeOwner;
        this.gpsVisible = gpsVisible;
        this.imageUris = imageUris;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }

    public String getNickname() {
        return nickname;
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

    public int getLikeCount() {
        return likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }
}