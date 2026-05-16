package com.neostride.app.feature.feed.model;

/*
 * 피드 상세 화면에서 사용할 댓글 응답 DTO 클래스임
 * 댓글 1개의 정보를 저장함
 */
public class FeedCommentResponse {

    // 댓글 고유 ID임
    private Long commentId;

    // 댓글 작성자 고유 ID임
    private Long writerId;

    // 댓글 작성자 닉네임임
    private String nickname;

    // 댓글 작성자 프로필 이미지 URL임
    private String profileImageUrl;

    // 댓글 내용임
    private String content;

    // 댓글 작성 시간임
    private String createdAt;

    // 현재 로그인한 사용자가 작성한 댓글인지 여부임
    private boolean mine;

    // 기본 생성자임
    public FeedCommentResponse() {
    }

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

    public boolean isMine() {
        return mine;
    }
}