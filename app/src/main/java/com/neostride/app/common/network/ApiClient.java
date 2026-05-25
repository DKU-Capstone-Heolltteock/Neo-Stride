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
             // API 호출 로그 (Logcat에서 확인 가능)
             HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
             logging.setLevel(HttpLoggingInterceptor.Level.BODY);

             OkHttpClient client = new OkHttpClient.Builder()
                     .addInterceptor(chain -> {
                         Request original = chain.request();
                         String path = original.url().encodedPath();

                         // 로그인/회원가입/토큰갱신은 토큰 불필요
                         if (path.contains("/auth/login") || path.contains("/auth/signup")
                                 || path.contains("/auth/email") || path.contains("/auth/find")
                                 || path.contains("/auth/reset") || path.contains("/auth/refresh")) {
                             return chain.proceed(original);
                         }

                         // 저장된 JWT 토큰 가져와서 헤더에 추가
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
                     // 401 응답 시 refresh token으로 자동 갱신 후 재시도
                     .authenticator((route, response) -> {
                         // refresh 요청 자체가 401이면 포기 (무한 루프 방지)
                         if (response.request().url().encodedPath().contains("/auth/refresh")) {
                             return null;
                         }

                         if (appContext == null) return null;

                         String refreshToken = TokenManager.getRefreshToken(appContext);
                         if (refreshToken == null || refreshToken.isEmpty()) {
                             redirectToLogin();
                             return null;
                         }

                         // 토큰 갱신을 동기 방식으로 시도
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
                                 String newAccessToken = body.getAccessToken();
                                 String newRefreshToken = body.getRefreshToken();

                                 // 새 토큰 저장
                                 TokenManager.saveTokens(appContext, newAccessToken, newRefreshToken);

                                 // 원래 요청에 새 토큰 붙여서 재시도
                                 return response.request().newBuilder()
                                         .header("Authorization", "Bearer " + newAccessToken)
                                         .build();
                             } else {
                                 // 갱신 실패 (refresh token도 만료) → 로그아웃
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

     // refresh token도 만료된 경우 자동 로그아웃 처리
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
