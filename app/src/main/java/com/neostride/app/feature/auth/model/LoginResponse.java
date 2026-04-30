package com.neostride.app.feature.auth.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    private String status;
    private String message;

    @SerializedName("user_id")
    private int userId;

    private String email;
    private String name;
    private String nickname;

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public int getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}