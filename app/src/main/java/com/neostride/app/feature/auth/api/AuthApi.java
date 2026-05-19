package com.neostride.app.feature.auth.api;

import com.neostride.app.feature.auth.model.LoginRequest;
import com.neostride.app.feature.auth.model.LoginResponse;
import com.neostride.app.feature.auth.model.SignupRequest;
import com.neostride.app.feature.auth.model.SignupResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface AuthApi {

    // 로그인
    @POST("/api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // 회원가입 (이미지 없이 JSON으로)
    @POST("/api/auth/signup")
    Call<SignupResponse> signup(@Body SignupRequest request);

    // 회원가입 (프로필 이미지 포함 multipart/form-data)
    // 백엔드 수신 필드: email, name, password (text), profile_photo (file, 선택)
    @Multipart
    @POST("/api/auth/signup")
    Call<SignupResponse> signupWithPhoto(
            @Part("email")    RequestBody email,
            @Part("name")     RequestBody name,
            @Part("password") RequestBody password,
            @Part            MultipartBody.Part profilePhoto  // nullable 시 생략
    );
}