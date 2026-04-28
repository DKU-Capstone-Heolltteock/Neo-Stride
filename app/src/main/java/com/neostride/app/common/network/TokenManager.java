package com.neostride.app.common.network;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "neo_stride_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 사용자 ID 가져오기 (없으면 일단 테스트용으로 1 반환)
    public static int getUserId(Context context) {
        return (int) getPrefs(context).getLong(KEY_USER_ID, 1);
    }

    public static String getAccessToken(Context context) {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, "");
    }
}
