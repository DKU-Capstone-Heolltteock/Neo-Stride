package com.neostride.app.feature.auth.api;

import com.neostride.app.feature.auth.model.LoginRequest;
import com.neostride.app.feature.auth.model.LoginResponse;
import com.neostride.app.feature.auth.model.SignupRequest;
import com.neostride.app.feature.auth.model.SignupResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    // 로그인
    @POST("/api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // 회원가입
    @POST("/api/auth/signup")
    Call<SignupResponse> signup(@Body SignupRequest request);
}