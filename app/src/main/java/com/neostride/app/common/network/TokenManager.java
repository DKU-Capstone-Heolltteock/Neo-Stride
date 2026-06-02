package com.neostride.app.common.network;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "neo_stride_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_KEEP_LOGIN = "keep_login";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── 토큰 저장 (로그인 유지 체크 시 호출 — refreshToken + keep_login 플래그 포함) ──
    public static void saveTokens(Context context, String accessToken, String refreshToken) {
        getPrefs(context).edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putBoolean(KEY_KEEP_LOGIN, true)
                .apply();
    }

    // ── 세션 토큰만 저장 (로그인 유지 미체크 시 — refreshToken은 저장하지 않음) ──
    public static void saveSessionToken(Context context, String accessToken) {
        getPrefs(context).edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .remove(KEY_REFRESH_TOKEN)
                .putBoolean(KEY_KEEP_LOGIN, false)
                .apply();
    }

    // ── 유저 정보 저장 ──
    public static void saveUserInfo(Context context, int userId, String nickname) {
        getPrefs(context).edit()
                .putInt(KEY_USER_ID, userId)   // putInt: getUserId의 getInt와 타입 통일
                .putString(KEY_NICKNAME, nickname)
                .apply();
    }

    // ── 로그인 유지 여부 ──
    public static boolean isKeepLogin(Context context) {
        return getPrefs(context).getBoolean(KEY_KEEP_LOGIN, false);
    }

    // ── 조회 ──
    public static String getAccessToken(Context context) {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, "");
    }

    public static String getRefreshToken(Context context) {
        return getPrefs(context).getString(KEY_REFRESH_TOKEN, "");
    }

    public static int getUserId(Context context) {
        // getInt로 읽기 (saveUserInfo의 putInt와 타입 통일)
        //  구버전이 putLong으로 저장했다면 getInt가 ClassCastException을 던지므로 try-catch로 감싸야 한다.
        //  (예외를 안 잡으면 LoginActivity.onCreate에서 그대로 터져 앱이 부팅 시 즉시 크래시한다.)
        SharedPreferences prefs = getPrefs(context);
        try {
            int fromInt = prefs.getInt(KEY_USER_ID, 0);
            if (fromInt > 0) return fromInt;
            // 값이 0이면 long fallback도 한 번 더 시도
            try {
                long fromLong = prefs.getLong(KEY_USER_ID, 0L);
                return (int) fromLong;
            } catch (ClassCastException ignored) {
                return 0;
            }
        } catch (ClassCastException e) {
            // 기존 값이 Long으로 저장돼 있던 경우 — long으로 읽고 int로 영구 마이그레이션
            try {
                long fromLong = prefs.getLong(KEY_USER_ID, 0L);
                int migrated = (int) fromLong;
                prefs.edit().putInt(KEY_USER_ID, migrated).apply();
                return migrated;
            } catch (ClassCastException ignored) {
                return 0;
            }
        }
    }

    public static String getNickname(Context context) {
        return getPrefs(context).getString(KEY_NICKNAME, "");
    }

    // ── 로그인 여부 ──
    public static boolean isLoggedIn(Context context) {
        String token = getAccessToken(context);
        return token != null && !token.isEmpty();
    }

    // ── 로그아웃 ──
    public static void clearTokens(Context context) {
        getPrefs(context).edit().clear().apply();
    }
}