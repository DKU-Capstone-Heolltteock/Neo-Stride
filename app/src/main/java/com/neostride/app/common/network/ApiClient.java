package com.neostride.app.common.network;

import android.content.Context;
import android.content.Intent;

import com.neostride.app.BuildConfig;
import com.neostride.app.feature.auth.LoginActivity;
import com.neostride.app.feature.auth.api.AuthApi;
import com.neostride.app.feature.auth.model.LoginResponse;
import com.neostride.app.feature.auth.model.RefreshRequest;

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

                        String refreshToken = TokenManager.getRefreshToken(appContext);
                        if (refreshToken == null || refreshToken.isEmpty()) {
                            redirectToLogin();
                            return null;
                        }

                        try {
                            AuthApi authApi = new Retrofit.Builder()
                                    .baseUrl(BASE_URL)
                                    .client(new OkHttpClient())
                                    .addConverterFactory(GsonConverterFactory.create())
                                    .build()
                                    .create(AuthApi.class);

                            Response<LoginResponse> refreshResponse =
                                    authApi.refresh(new RefreshRequest(refreshToken)).execute();

                            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                                LoginResponse body = refreshResponse.body();
                                TokenManager.saveTokens(appContext, body.getAccessToken(), body.getRefreshToken());
                                return response.request().newBuilder()
                                        .header("Authorization", "Bearer " + body.getAccessToken())
                                        .build();
                            } else {
                                redirectToLogin();
                                return null;
                            }
                        } catch (IOException e) {
                            return null;
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