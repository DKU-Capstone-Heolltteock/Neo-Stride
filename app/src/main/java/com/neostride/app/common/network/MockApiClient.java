package com.neostride.app.common.network;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/*
 * 개발용 Mock Retrofit Client 클래스임
 * MockInterceptor를 사용해 실제 서버 대신 가짜 JSON 응답을 반환함
 *
 * push/merge 전에는 FeedRepository에서 다시 ApiClient를 사용하도록 되돌려야 함
 */
public class MockApiClient {

    private static Retrofit retrofit = null;

    /*
     * Mock Retrofit 인스턴스를 반환함
     */
    public static Retrofit getInstance() {
        if (retrofit == null) {

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new MockInterceptor())
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl("http://127.0.0.1/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}