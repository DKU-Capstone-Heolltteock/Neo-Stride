package com.neostride.app.feature.community.feed.model;

/*
 * 피드 댓글 작성 요청 DTO 클래스임
 * POST /api/community/feeds/{feedId}/comments 요청 Body에 담을 데이터를 저장함
 */
public class FeedCommentRequest {

    // 작성할 댓글 내용임
    private String content;

    /*
     * 댓글 내용을 받아 요청 객체를 생성하는 생성자임
     */
    public FeedCommentRequest(String content) {
        this.content = content;
    }

    /*
     * 댓글 내용을 반환하는 함수임
     */
    public String getContent() {
        return content;
    }
}