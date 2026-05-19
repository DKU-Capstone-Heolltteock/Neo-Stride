package com.neostride.app.feature.auth.repository;

// API 인터페이스 (서버 주소 정의)
import com.neostride.app.feature.auth.api.AuthApi;

// 요청 / 응답 데이터 모델
import com.neostride.app.feature.auth.model.LoginRequest;
import com.neostride.app.feature.auth.model.LoginResponse;
import com.neostride.app.feature.auth.model.SignupRequest;
import com.neostride.app.feature.auth.model.SignupResponse;

// Retrofit 관련 라이브러리
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//주소
import com.neostride.app.BuildConfig;

public class AuthRepository {

    // 서버 API를 사용할 객체 (AuthApi 인터페이스 기반)
    private final AuthApi authApi;

    // 생성자 → Repository가 만들어질 때 Retrofit 세팅 수행
    public AuthRepository() {

        // Retrofit 객체 생성
        Retrofit retrofit = new Retrofit.Builder()
                // 서버 기본 주소 (에뮬레이터에서 localhost 접근 시 10.0.2.2 사용)
                //.baseUrl(BuildConfig.BASE_URL)
                .baseUrl(BuildConfig.BASE_URL)
                // JSON ↔ Java 객체 자동 변환 (Gson 사용)
                .addConverterFactory(GsonConverterFactory.create())

                // Retrofit 객체 생성 완료
                .build();

        // AuthApi 인터페이스를 실제 동작하는 객체로 변환
        authApi = retrofit.create(AuthApi.class);
    }

    // ===========================
    // 로그인 API 호출 함수
    // ===========================
    public void login(LoginRequest request, Callback<LoginResponse> callback) {

        // authApi에 정의된 login API 호출
        // request → 서버로 보낼 데이터 (ID, 비밀번호)
        // enqueue → 비동기 요청 (UI 멈추지 않음)
        authApi.login(request).enqueue(callback);
    }

    // ===========================
    // 회원가입 API 호출 함수 (이미지 없음)
    // ===========================
    public void signup(SignupRequest request, Callback<SignupResponse> callback) {
        authApi.signup(request).enqueue(callback);
    }

    // ===========================
    // 회원가입 API 호출 함수 (프로필 이미지 포함)
    // ===========================
    public void signupWithPhoto(RequestBody email, RequestBody name, RequestBody password,
                                MultipartBody.Part profilePhoto, Callback<SignupResponse> callback) {
        authApi.signupWithPhoto(email, name, password, profilePhoto).enqueue(callback);
    }
}