package com.neostride.app.feature.auth;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.neostride.app.R;
import com.neostride.app.feature.main.MainActivity;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.auth.model.LoginRequest;
import com.neostride.app.feature.auth.model.LoginResponse;
import com.neostride.app.feature.auth.repository.AuthRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etId;
    private EditText etPw;
    private CheckBox cbKeepLogin;
    private Button btnLogin;
    private TextView tvRegister;
    private TextView tvFindAccount;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 로그인 유지 체크 상태에서만 자동 로그인
        if (TokenManager.isKeepLogin(this) && !TokenManager.getAccessToken(this).isEmpty()) {
            if (TokenManager.getUserId(this) > 0) {
                // userId가 정상적으로 저장된 경우에만 자동 로그인
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            } else {
                // userId가 0이면 데이터 손실 — 토큰 초기화 후 수동 로그인 유도
                TokenManager.clearTokens(this);
            }
        }

        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 화면 요소 연결함
        etId = findViewById(R.id.et_id);
        etPw = findViewById(R.id.et_pw);
        cbKeepLogin = findViewById(R.id.cb_keep_login);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        tvFindAccount = findViewById(R.id.tv_find);

        // "Register here" 부분만 굵게 표시
        String registerText = "Don't have an account yet? Register here";
        SpannableString spannable = new SpannableString(registerText);
        int boldStart = registerText.indexOf("Register here");
        spannable.setSpan(new StyleSpan(Typeface.BOLD), boldStart, registerText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvRegister.setText(spannable);

        // Repository 생성함
        authRepository = new AuthRepository();

        // 로그인 버튼 클릭 시 로그인 함수 실행함
        btnLogin.setOnClickListener(v -> login());

        // 회원가입 화면으로 이동함
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // 아이디/비밀번호 찾기 기능은 임시 비활성화함
        tvFindAccount.setEnabled(false);
        tvFindAccount.setClickable(false);
        tvFindAccount.setAlpha(0.5f);
    }

    private void login() {
        String email = etId.getText().toString().trim();
        String password = etPw.getText().toString().trim();
        boolean keepLogin = cbKeepLogin.isChecked();

        // 이메일 입력 여부 검사함
        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 비밀번호 입력 여부 검사함
        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("LOGIN", "로그인 요청");
        Log.d("LOGIN", "email: " + email);

        // 서버로 보낼 로그인 요청 객체 생성함
        LoginRequest request = new LoginRequest(email, password);

        // 로그인 API 호출함
        authRepository.login(request, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                Log.d("LOGIN", "응답 코드: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    Log.d("LOGIN", "로그인 성공");
                    Log.d("LOGIN", "User ID: " + loginResponse.getUserId());

                    // 유저 정보와 accessToken은 항상 저장 (현재 세션 API 호출에 필요)
                    // refreshToken은 로그인 유지 체크 시에만 저장 (다음 앱 실행 자동 로그인용)
                    if (keepLogin) {
                        TokenManager.saveTokens(
                                LoginActivity.this,
                                loginResponse.getAccessToken(),
                                loginResponse.getRefreshToken()
                        );
                    } else {
                        TokenManager.saveSessionToken(
                                LoginActivity.this,
                                loginResponse.getAccessToken()
                        );
                    }

                    TokenManager.saveUserInfo(
                            LoginActivity.this,
                            loginResponse.getUserId(),
                            loginResponse.getNickname()
                    );

                    Log.d("LOGIN", "TokenManager 저장 완료");

                    Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();

                    // 메인 화면으로 이동함
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();

                } else if (response.code() == 400) {
                    Log.d("LOGIN", "요청 형식 오류");
                    Toast.makeText(LoginActivity.this, "입력값을 확인해주세요.", Toast.LENGTH_SHORT).show();

                } else if (response.code() == 401) {
                    Log.d("LOGIN", "이메일 또는 비밀번호 불일치");
                    Toast.makeText(LoginActivity.this, "이메일 또는 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();

                } else {
                    Log.d("LOGIN", "로그인 실패");
                    Toast.makeText(LoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.d("LOGIN", "서버 연결 실패");
                Toast.makeText(LoginActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }
}