package com.neostride.app.feature.feed.model;

public class TagUser {

    private Long userId;
    private String nickname;

    public TagUser(Long userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }
}