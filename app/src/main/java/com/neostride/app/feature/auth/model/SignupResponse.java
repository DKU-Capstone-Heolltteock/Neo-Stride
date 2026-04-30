package com.neostride.app.feature.auth.model;

import com.google.gson.annotations.SerializedName;

public class SignupResponse {

    private String status;
    private String message;

    @SerializedName("user_id")
    private int userId;

    private String email;
    private String name;

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
}