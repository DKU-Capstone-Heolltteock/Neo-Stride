package com.neostride.app.feature.notification.api;

import com.neostride.app.feature.notification.model.NotificationResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

/*
 * 알림 관련 API 인터페이스임
 * Retrofit이 이 인터페이스를 기반으로 실제 HTTP 통신 코드를 자동 생성함
 */
public interface NotificationApi {

    /*
     * 알림 목록 조회 API임
     * GET /api/notifications
     *
     * @Header("X-User-Id") : 현재 로그인한 사용자 ID
     */
    @GET("api/notifications")
    Call<List<NotificationResponse>> getNotifications(
            @Header("X-User-Id") Long userId
    );

    /*
     * 알림 단건 삭제 API임
     * DELETE /api/notifications/{notificationId}
     */
    @DELETE("api/notifications/{notificationId}")
    Call<okhttp3.ResponseBody> deleteNotification(
            @Path("notificationId") Long notificationId
    );

    /*
     * 알림 전체 삭제 API임
     * DELETE /api/notifications
     */
    @DELETE("api/notifications")
    Call<okhttp3.ResponseBody> deleteAllNotifications(
            @Header("X-User-Id") Long userId
    );
}
