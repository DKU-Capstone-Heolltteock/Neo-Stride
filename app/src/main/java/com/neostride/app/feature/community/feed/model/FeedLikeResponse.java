package com.neostride.app.feature.community.feed.model;

/*
 * 피드 좋아요 토글 응답 DTO 클래스임
 * POST /api/community/feeds/{feedId}/likes 응답 데이터를 저장함
 */
public class FeedLikeResponse {

    // 좋아요가 적용된 피드 ID임
    private Long feedId;

    // 현재 로그인한 사용자가 좋아요를 눌렀는지 여부임
    private boolean liked;

    // 좋아요 토글 후의 최신 좋아요 수임
    private int likeCount;

    // 기본 생성자임
    public FeedLikeResponse() {
    }

    public Long getFeedId() {
        return feedId;
    }

    public boolean isLiked() {
        return liked;
    }

    public int getLikeCount() {
        return likeCount;
    }
}