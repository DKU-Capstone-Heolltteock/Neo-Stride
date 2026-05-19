package com.neostride.app.feature.community.tip.model;

/*
 * 팁 댓글 작성 요청 DTO 클래스임
 * 사용자가 입력한 댓글 내용을 서버로 전송함
 */
public class TipCommentRequest {

    private String content;

    public TipCommentRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}