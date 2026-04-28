package com.neostride.app.common.network;

import com.neostride.app.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // 추후 팀장님 백엔드 담당 해주는 본체에 해당하는 ip 주소 넣기
    // ex) private static final String BASE_URL = "http://10.0.2.2:8080/";
    // 추후 필요시 Gradle Scripts/local.properties 에 URL 변경하기
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;

    public static Retrofit getInstance() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        // 보낼 때마다 TokenManager에서 토큰 꺼내서 헤더에 붙여줌
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + "dummy_token")
                                .build();
                        return chain.proceed(request);
                    }).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}