package com.neostride.app.feature.notification;

/*
 * 알림 아이템 모델 클래스임
 */
public class NotificationItem {

    public static final String TYPE_FEED_TAG       = "FEED_TAG";
    public static final String TYPE_FRIEND_REQUEST = "FRIEND_REQUEST";
    public static final String TYPE_GRADE          = "GRADE";
    public static final String TYPE_FEED_COMMENT   = "FEED_COMMENT";  // 피드 댓글
    public static final String TYPE_TIP_COMMENT    = "TIP_COMMENT";  // 팁 댓글
    public static final String TYPE_FEED_LIKE      = "FEED_LIKE";    // 피드 좋아요
    public static final String TYPE_TIP_LIKE       = "TIP_LIKE";     // 팁 좋아요

    public final Long   notificationId;
    public final String type;
    public final String message;
    public final String time;

    // 이동할 대상 ID (feedId, tipId 등 — 타입에 따라 다름)
    public final Long   targetId;

    /*
     * 서버 기준 읽음 여부 + 앱 세션 내 열람 여부를 합쳐서 판단함
     * true  → 이미 확인한 알림 (회색)
     * false → 새 알림 (형광)
     */
    public boolean read;

    public NotificationItem(Long notificationId, String type, String message, String time, Long targetId, boolean read) {
        this.notificationId = notificationId;
        this.type           = type;
        this.message        = message;
        this.time           = time;
        this.targetId       = targetId;
        this.read           = read;
    }
}
