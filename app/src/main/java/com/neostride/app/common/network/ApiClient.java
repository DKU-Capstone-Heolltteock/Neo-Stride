package com.neostride.app.common.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // 추후 팀장님 백엔드 담당 해주는 본체에 해당하는 ip 주소 넣기
    // ex) private static final String BASE_URL = "http://10.0.2.2:8080/";
    // 현재 postman으로 신호가 가는지 확인 가능
    private static final String BASE_URL = "https://6b068f2e-91de-4c62-be70-f8302ba5e407.mock.pstmn.io/";
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