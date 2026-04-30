package com.neostride.app.feature.auth;

import android.content.Intent;
import android.os.Bundle;
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
import com.neostride.app.activity.MainActivity;
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
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 화면 요소 연결
        etId = findViewById(R.id.et_id);
        etPw = findViewById(R.id.et_pw);
        cbKeepLogin = findViewById(R.id.cb_keep_login);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        tvFindAccount = findViewById(R.id.tv_find);

        // Repository 생성
        authRepository = new AuthRepository();

        // 로그인 버튼 클릭
        btnLogin.setOnClickListener(v -> login());

        // 회원가입 화면 이동
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // ID/비밀번호 찾기 기능 임시 비활성화
        tvFindAccount.setEnabled(false);
        tvFindAccount.setClickable(false);
        tvFindAccount.setAlpha(0.5f);

        /*
        tvFindAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });
        */
    }

    private void login() {
        String email = etId.getText().toString().trim();
        String password = etPw.getText().toString().trim();
        boolean keepLogin = cbKeepLogin.isChecked();

        // 입력값 검사
        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 로그 출력 (비밀번호는 출력하지 않음)
        Log.d("LOGIN", "로그인 요청");
        Log.d("LOGIN", "email: " + email);

        // 서버로 보낼 로그인 요청 객체 생성
        LoginRequest request = new LoginRequest(email, password);

        // 로그인 API 호출
        authRepository.login(request, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                Log.d("LOGIN", "응답 코드: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    Log.d("LOGIN", "로그인 성공");
                    Log.d("LOGIN", "message: " + loginResponse.getMessage());

                    Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();

                    // 로그인 유지 체크 시 access token 저장
                    if (keepLogin && loginResponse.getAccessToken() != null) {
                        getSharedPreferences("auth", MODE_PRIVATE)
                                .edit()
                                .putString("access_token", loginResponse.getAccessToken())
                                .putString("refresh_token", loginResponse.getRefreshToken())
                                .apply();

                        Log.d("LOGIN", "토큰 저장 완료");
                    }

                    // 메인 화면으로 이동
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
                Log.e("LOGIN", "요청 실패: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }
}