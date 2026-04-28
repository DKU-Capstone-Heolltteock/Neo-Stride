package com.neostride.app.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;

public class SignupActivity extends AppCompatActivity {

    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, EmailAuthActivity.class);
            startActivity(intent);
        });
    }
}