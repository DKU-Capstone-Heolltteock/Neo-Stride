package com.neostride.app.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.neostride.app.R;

import android.content.Intent;
import android.widget.TextView;

public class ResetPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        // 🔥 이거 추가
        Button btnResetContinue = findViewById(R.id.btnResetContinue);

        btnResetContinue.setOnClickListener(v -> {
            Intent intent = new Intent(ResetPasswordActivity.this, ResetCodeVerifyActivity.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvFindId = findViewById(R.id.tvFindId);

        tvFindId.setOnClickListener(v -> {
            Intent intent = new Intent(ResetPasswordActivity.this, FindIdActivity.class);
            startActivity(intent);
        });

        tvFindId.setOnClickListener(v -> {
            Intent intent = new Intent(ResetPasswordActivity.this, FindIdActivity.class);
            startActivity(intent);
        });
    }
}