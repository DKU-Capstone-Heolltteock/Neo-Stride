package com.neostride.app.feature.community.feed.model;

/*
 * 피드 북마크 토글 응답 DTO 클래스임
 * POST /api/community/feeds/{feedId}/bookmarks 응답 데이터를 저장함
 */
public class FeedBookmarkResponse {

    // 북마크가 적용된 피드 ID임
    private Long feedId;

    // 현재 로그인한 사용자가 북마크를 눌렀는지 여부임
    private boolean bookmarked;

    // 기본 생성자임
    public FeedBookmarkResponse() {
    }

    public Long getFeedId() {
        return feedId;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }
}