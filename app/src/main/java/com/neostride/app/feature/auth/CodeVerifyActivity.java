package com.neostride.app.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;

public class CodeVerifyActivity extends AppCompatActivity {

    private Button btnVerifyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_verify);

        btnVerifyCode = findViewById(R.id.btn_verify_code);

        btnVerifyCode.setOnClickListener(v -> {
            Intent intent = new Intent(CodeVerifyActivity.this, SuccessActivity.class);
            startActivity(intent);
        });
    }
}