package com.neostride.app.feature.tip.model;

/*
 * 팁 댓글 응답 DTO 클래스임
 * 팁 상세 조회 응답 안의 댓글 1개 데이터를 저장함
 */
public class TipCommentResponse {

    // 댓글 ID임
    private Long commentId;

    // 댓글 작성자 ID임
    private Long writerId;

    // 댓글 작성자 닉네임임
    private String nickname;

    // 댓글 작성자 프로필 이미지 URL임
    private String profileImageUrl;

    // 댓글 내용임
    private String content;

    // 댓글 작성 시간임
    private String createdAt;

    // 댓글 작성자 배지 보유 여부임
    private boolean badgeOwned;

    // 댓글 작성자 배지 등급임 (bronze/silver/gold/platinum/diamond/master/challenger)
    private String badgeType;

    // 현재 로그인한 사용자가 작성한 댓글인지 여부임
    private boolean mine;

    public Long getCommentId() {
        return commentId;
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

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isBadgeOwned() {
        return badgeOwned;
    }

    public String getBadgeType() {
        return badgeType;
    }

    public boolean isMine() {
        return mine;
    }
}