// package com.neostride.app.common.network;

// <<< 기존 정상 코드 >>>

// import android.content.Context;

// import com.neostride.app.BuildConfig;

// import okhttp3.OkHttpClient;
// import okhttp3.Request;
// import okhttp3.logging.HttpLoggingInterceptor;
// import retrofit2.Retrofit;
// import retrofit2.converter.gson.GsonConverterFactory;

// import java.util.concurrent.TimeUnit;

// public class ApiClient {

//     // BASE_URL 은 local.properties에 입력할것
//     private static final String BASE_URL = BuildConfig.BASE_URL;
//     private static Retrofit retrofit = null;
//     private static Context appContext = null;

//     // 앱 시작 시 한 번 호출 (MainActivity onCreate에서)
//     public static void init(Context context) {
//         appContext = context.getApplicationContext();
//     }

//     public static Retrofit getInstance() {
//         if (retrofit == null) {
//             // API 호출 로그 (Logcat에서 확인 가능)
//             HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
//             logging.setLevel(HttpLoggingInterceptor.Level.BODY);

//             OkHttpClient client = new OkHttpClient.Builder()
//                     .addInterceptor(chain -> {
//                         Request original = chain.request();
//                         String path = original.url().encodedPath();

//                         // 로그인/회원가입은 토큰 불필요
//                         if (path.contains("/auth/login") || path.contains("/auth/signup")
//                                 || path.contains("/auth/email") || path.contains("/auth/find")
//                                 || path.contains("/auth/reset")) {
//                             return chain.proceed(original);
//                         }

//                         // 저장된 JWT 토큰 가져와서 헤더에 추가
//                         String token = "";
//                         if (appContext != null) {
//                             token = TokenManager.getAccessToken(appContext);
//                         }

//                         if (token != null && !token.isEmpty()) {
//                             Request authorized = original.newBuilder()
//                                     .header("Authorization", "Bearer " + token)
//                                     .build();
//                             return chain.proceed(authorized);
//                         }

//                         return chain.proceed(original);
//                     })
//                     .addInterceptor(logging)
//                     .connectTimeout(15, TimeUnit.SECONDS)
//                     .readTimeout(15, TimeUnit.SECONDS)
//                     .writeTimeout(15, TimeUnit.SECONDS)
//                     .build();

//             retrofit = new Retrofit.Builder()
//                     .baseUrl(BASE_URL)
//                     .client(client)
//                     .addConverterFactory(GsonConverterFactory.create())
//                     .build();
//         }
//         return retrofit;
//     }

//     public static void resetInstance() {
//         retrofit = null;
//     }
// }

package com.neostride.app.common.network;

import android.content.Context;
import android.util.Log;

import com.neostride.app.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;
    private static Context appContext = null;

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

                        // URL 로깅 로직
                        String fullUrl = original.url().toString();
                        Log.d("NeoStride_URL_Check", "============================================");
                        Log.d("NeoStride_URL_Check", "🚀 실제 요청 URL: " + fullUrl);
                        Log.d("NeoStride_URL_Check", "============================================");

                        String path = original.url().encodedPath();

                        if (path.contains("/auth/login") || path.contains("/auth/signup")
                                || path.contains("/auth/email") || path.contains("/auth/find")
                                || path.contains("/auth/reset")) {
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
                    // 가짜 우체국(Mockserver)
                    // 이 위치에 두면 URL 로그는 찍히되, 실제 네트워크 에러가 나기 전에 데이터를 꽂아줍니다.
                    //.addInterceptor(new Mockserver())
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

    public static void resetInstance() {
        retrofit = null;
    }
}