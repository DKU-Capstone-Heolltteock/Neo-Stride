package com.neostride.app.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;

public class SuccessActivity extends AppCompatActivity {

    private Button btnContinueLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        btnContinueLogin = findViewById(R.id.btn_continue_login);

        btnContinueLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SuccessActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}