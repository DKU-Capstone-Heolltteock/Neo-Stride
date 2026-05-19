package com.neostride.app.feature.notification.model;

/*
 * 알림 응답 DTO 클래스임
 * 서버에서 알림 목록 조회 시 반환하는 데이터를 저장함
 *
 * type 값:
 *   FEED_TAG       - 피드에서 나를 태그한 경우
 *   FRIEND_REQUEST - 친구 요청을 받은 경우
 *   GRADE          - 신규 등급 달성
 *   COMMENT        - 내 글(피드/팁)에 댓글
 *   LIKE           - 내 글(피드/팁)에 좋아요
 */
public class NotificationResponse {

    // 알림 고유 ID
    private Long notificationId;

    // 알림 타입 (FEED_TAG / FRIEND_REQUEST / GRADE / COMMENT / LIKE)
    private String type;

    // 알림 메시지 (예: "OOO님이 회원님을 피드에서 태그했습니다")
    private String message;

    // 알림 발생 시각 (ISO 8601 형식)
    private String createdAt;

    // 알림을 발생시킨 대상 ID (feedId, tipId 등 — 타입에 따라 다름)
    private Long targetId;

    // 읽음 여부
    private boolean read;

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
