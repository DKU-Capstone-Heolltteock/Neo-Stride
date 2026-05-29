package com.neostride.app.common.network;

import android.content.Context;
import android.content.Intent;

import com.neostride.app.BuildConfig;
import com.neostride.app.feature.auth.LoginActivity;
import com.neostride.app.feature.auth.api.AuthApi;
import com.neostride.app.feature.auth.model.LoginResponse;
import com.neostride.app.feature.auth.model.RefreshRequest;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // BASE_URL 은 local.properties에 입력할것
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;
    private static Context appContext = null;
    // 동시 다발 401 → 중복 refresh 방지용 락
    private static final Object REFRESH_LOCK = new Object();

    // 앱 시작 시 한 번 호출 (MainActivity onCreate에서)
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit getInstance() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        String path = original.url().encodedPath();

                        if (path.contains("/auth/login") || path.contains("/auth/signup")
                                || path.contains("/auth/email") || path.contains("/auth/find")
                                || path.contains("/auth/reset") || path.contains("/auth/refresh")) {
                            return chain.proceed(original);
                        }

                        String token = "";
                        if (appContext != null) {
                            token = TokenManager.getAccessToken(appContext);
                        }

                        if (token != null && !token.isEmpty()) {
                            Request authorized = original.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                            return chain.proceed(authorized);
                        }

                        return chain.proceed(original);
                    })
                    .authenticator((route, response) -> {
                        if (response.request().url().encodedPath().contains("/auth/refresh")) {
                            return null;
                        }

                        if (appContext == null) return null;

                        synchronized (REFRESH_LOCK) {
                            // 이미 다른 스레드가 refresh를 완료했는지 확인
                            // → 실패한 요청에 쓴 token과 현재 저장된 token이 다르면 이미 갱신된 것
                            String currentAccessToken = TokenManager.getAccessToken(appContext);
                            String requestAuthHeader = response.request().header("Authorization");
                            if (requestAuthHeader != null
                                    && !currentAccessToken.isEmpty()
                                    && !requestAuthHeader.equals("Bearer " + currentAccessToken)) {
                                Log.d("ApiClient", "[AUTH] 이미 다른 스레드가 refresh 완료 → 새 token으로 재시도");
                                return response.request().newBuilder()
                                        .header("Authorization", "Bearer " + currentAccessToken)
                                        .build();
                            }

                            Log.d("ApiClient", "[AUTH] 401 발생 URL: " + response.request().url());

                            String refreshToken = TokenManager.getRefreshToken(appContext);
                            if (refreshToken == null || refreshToken.isEmpty()) {
                                Log.e("ApiClient", "[AUTH] refresh token 없음 → redirectToLogin");
                                redirectToLogin();
                                return null;
                            }

                            Log.d("ApiClient", "[AUTH] refresh 시도...");

                            try {
                                AuthApi authApi = new Retrofit.Builder()
                                        .baseUrl(BASE_URL)
                                        .client(new OkHttpClient())
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build()
                                        .create(AuthApi.class);

                                Response<LoginResponse> refreshResponse =
                                        authApi.refresh(new RefreshRequest(refreshToken)).execute();

                                Log.d("ApiClient", "[AUTH] refresh 응답 코드: " + refreshResponse.code());

                                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                                    LoginResponse body = refreshResponse.body();
                                    TokenManager.saveTokens(appContext, body.getAccessToken(), body.getRefreshToken());
                                    // 토큰 갱신 시 userId/nickname도 함께 갱신 (기기 저장값 최신화)
                                    if (body.getUserId() > 0) {
                                        TokenManager.saveUserInfo(appContext, body.getUserId(), body.getNickname());
                                    }
                                    Log.d("ApiClient", "[AUTH] refresh 성공 → 원래 요청 재시도");
                                    return response.request().newBuilder()
                                            .header("Authorization", "Bearer " + body.getAccessToken())
                                            .build();
                                } else {
                                    // 401/403: refresh 토큰 자체가 무효 → 로그아웃
                                    // 5xx 등 서버 일시 장애: 강제 로그아웃하지 않고 요청만 실패 처리
                                    int code = refreshResponse.code();
                                    Log.e("ApiClient", "[AUTH] refresh 실패 코드=" + code + (code == 401 || code == 403 ? " → redirectToLogin" : " → 요청만 실패"));
                                    if (code == 401 || code == 403) {
                                        redirectToLogin();
                                    }
                                    return null;
                                }
                            } catch (IOException e) {
                                Log.e("ApiClient", "[AUTH] refresh 네트워크 오류: " + e.getMessage());
                                return null;
                            }
                        }
                    })
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    private static void redirectToLogin() {
        if (appContext == null) return;
        TokenManager.clearTokens(appContext);
        Intent intent = new Intent(appContext, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appContext.startActivity(intent);
    }

    public static void resetInstance() {
        retrofit = null;
    }
}