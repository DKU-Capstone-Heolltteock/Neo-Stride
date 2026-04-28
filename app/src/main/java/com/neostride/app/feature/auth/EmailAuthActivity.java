package com.neostride.app.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;

public class EmailAuthActivity extends AppCompatActivity {

    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_auth);

        btnContinue = findViewById(R.id.btn_continue);

        btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(EmailAuthActivity.this, CodeVerifyActivity.class);
            startActivity(intent);
        });
    }
}