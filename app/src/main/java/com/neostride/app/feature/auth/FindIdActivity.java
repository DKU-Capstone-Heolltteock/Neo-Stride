package com.neostride.app.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;

public class FindIdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);

        Button btnFindIdContinue = findViewById(R.id.btnFindIdContinue);

        btnFindIdContinue.setOnClickListener(v -> {
            Intent intent = new Intent(FindIdActivity.this, FindIdCodeVerifyActivity.class);
            startActivity(intent);
        });
    }
}