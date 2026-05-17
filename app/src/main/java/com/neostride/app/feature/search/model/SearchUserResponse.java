package com.neostride.app.feature.search.model;

/*
 * 프로필/친구 검색 결과 DTO 클래스임
 * 피드/팁은 기존 FeedResponse, TipResponse를 쓰고
 * 사용자 검색 결과만 별도 DTO로 받음
 */
public class SearchUserResponse {

    /*
     * 사용자 ID임
     * RunnerPageActivity 이동 시 user_id로 전달함
     */
    private Long userId;

    /*
     * 사용자 닉네임임
     */
    private String nickname;

    /*
     * 프로필 이미지 URL임
     */
    private String profileImageUrl;

    /*
     * 상태 메시지 또는 소개 문구임
     */
    private String statusMessage;

    /*
     * 친구 수임
     */
    private int friendCount;

    /*
     * 배지 등급임
     * GOLD, SILVER, BRONZE, NONE 등으로 사용함
     */
    private String badgeTier;

    /*
     * 친구 여부임
     */
    private boolean friend;

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getFriendCount() {
        return friendCount;
    }

    public String getBadgeTier() {
        return badgeTier;
    }

    public boolean isFriend() {
        return friend;
    }
}