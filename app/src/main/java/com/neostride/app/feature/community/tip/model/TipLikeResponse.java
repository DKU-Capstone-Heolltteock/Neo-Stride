package com.neostride.app.feature.community.tip.model;

/*
 * 팁 좋아요 토글 응답 DTO 클래스임
 * 좋아요 클릭 후 서버가 변경된 좋아요 상태와 개수를 반환함
 */
public class TipLikeResponse {

    private Long tipId;

    private boolean liked;

    private int likeCount;

    public Long getTipId() {
        return tipId;
    }

    public boolean isLiked() {
        return liked;
    }

    public int getLikeCount() {
        return likeCount;
    }
}