package com.walid.almuhfz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class NoInternetActivity extends AppCompatActivity {

    private AppCompatButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initializeViews();
        setupListeners();
        Toast.makeText(this, "يرجى التأكد من اتصالك بالإنترنت لاستخدام المصحف المعلم.", Toast.LENGTH_LONG).show();
    }

    private void initializeViews() {
        loginButton = findViewById(R.id.loginn);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> {
            startActivity(new Intent(NoInternetActivity.this, SplashActivity.class));
            finish();
        });
    }
}
