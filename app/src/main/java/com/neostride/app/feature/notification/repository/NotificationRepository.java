package com.neostride.app.feature.notification.repository;

import android.content.Context;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.notification.api.NotificationApi;
import com.neostride.app.feature.notification.model.NotificationResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 * 알림 관련 데이터 처리를 담당하는 Repository 클래스임
 * 실제 서버 API를 통해 알림 목록 조회 및 삭제를 처리함
 */
public class NotificationRepository {

    private final NotificationApi notificationApi;
    private final Context context;

    public NotificationRepository(Context context) {
        this.context = context.getApplicationContext();
        notificationApi = ApiClient.getInstance().create(NotificationApi.class);
    }

    /*
     * 알림 목록을 조회하는 함수임
     * GET /api/notifications
     */
    public void getNotifications(RepositoryCallback<List<NotificationResponse>> callback) {
        long userId = TokenManager.getUserId(context);

        notificationApi.getNotifications(userId).enqueue(new Callback<List<NotificationResponse>>() {
            @Override
            public void onResponse(
                    Call<List<NotificationResponse>> call,
                    Response<List<NotificationResponse>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<NotificationResponse>> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 알림 전체 읽음 처리 함수임
     * PATCH /api/notifications/read-all
     */
    public void markAllRead(RepositoryCallback<Boolean> callback) {
        long userId = TokenManager.getUserId(context);

        notificationApi.markAllRead(userId).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(
                    Call<okhttp3.ResponseBody> call,
                    Response<okhttp3.ResponseBody> response
            ) {
                if (response.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 알림 단건 삭제 함수임
     * DELETE /api/notifications/{notificationId}
     */
    public void deleteNotification(Long notificationId, RepositoryCallback<Boolean> callback) {
        notificationApi.deleteNotification(notificationId).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(
                    Call<okhttp3.ResponseBody> call,
                    Response<okhttp3.ResponseBody> response
            ) {
                if (response.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 알림 전체 삭제 함수임
     * DELETE /api/notifications
     */
    public void deleteAllNotifications(RepositoryCallback<Boolean> callback) {
        long userId = TokenManager.getUserId(context);

        notificationApi.deleteAllNotifications(userId).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(
                    Call<okhttp3.ResponseBody> call,
                    Response<okhttp3.ResponseBody> response
            ) {
                if (response.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * Repository 작업 결과를 화면으로 전달하기 위한 Callback 인터페이스임
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}
