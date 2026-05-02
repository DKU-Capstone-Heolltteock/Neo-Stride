package com.neostride.app.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;
import com.neostride.app.feature.auth.model.SignupRequest;
import com.neostride.app.feature.auth.model.SignupResponse;
import com.neostride.app.feature.auth.repository.AuthRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etName;
    private EditText etPassword;
    private Button btnRegister;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // 입력 필드 연결
        etEmail = findViewById(R.id.et_email);
        etName = findViewById(R.id.et_name);
        etPassword = findViewById(R.id.et_pw);
        btnRegister = findViewById(R.id.btn_register);

        // Repository 생성
        authRepository = new AuthRepository();

        // 회원가입 버튼 클릭
        btnRegister.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String name = etName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // 🔥 입력값 체크
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (name.isEmpty()) {
                Toast.makeText(this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔥 로그 출력 (비밀번호 제외)
            Log.d("SIGNUP", "회원가입 요청");
            Log.d("SIGNUP", "email: " + email);
            Log.d("SIGNUP", "name: " + name);

            // 요청 객체 생성
            SignupRequest request = new SignupRequest(email, name, password);

            // API 호출
            authRepository.signup(request, new Callback<SignupResponse>() {

                @Override
                public void onResponse(Call<SignupResponse> call, Response<SignupResponse> response) {

                    Log.d("SIGNUP", "응답 코드: " + response.code());

                    if (response.code() == 201) {
                        Log.d("SIGNUP", "회원가입 성공");

                        Toast.makeText(SignupActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();

                        // 성공 화면 이동
                        Intent intent = new Intent(SignupActivity.this, SuccessActivity.class);
                        startActivity(intent);
                        finish();

                    } else if (response.code() == 409) {
                        Log.d("SIGNUP", "이미 가입된 이메일");

                        Toast.makeText(SignupActivity.this, "이미 가입된 이메일입니다.", Toast.LENGTH_SHORT).show();

                    } else if (response.code() == 400) {
                        Log.d("SIGNUP", "요청 형식 오류");

                        Toast.makeText(SignupActivity.this, "입력값을 확인해주세요.", Toast.LENGTH_SHORT).show();

                    } else {
                        Log.d("SIGNUP", "기타 오류");

                        Toast.makeText(SignupActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<SignupResponse> call, Throwable t) {
                    Log.e("SIGNUP", "요청 실패: " + t.getMessage());

                    Toast.makeText(SignupActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}