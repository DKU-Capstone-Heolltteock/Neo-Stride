package com.neostride.app.feature.tip.model;

/*
 * 팁 북마크 토글 응답 DTO 클래스임
 * 북마크 클릭 후 서버가 변경된 북마크 상태를 반환함
 */
public class TipBookmarkResponse {

    private Long tipId;

    private boolean bookmarked;

    public Long getTipId() {
        return tipId;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }
}