package com.example.moodymusic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_activity);
        configureMainLogin();
        configureMainRegister();
    }

    private void configureMainLogin() {
        Button mainLogin = findViewById(R.id.mainLogin);
        mainLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, com.example.moodymusic.Login.class));
            }
        });
    }

    private void configureMainRegister() {
        Button mainRegister = findViewById(R.id.mainRegister);
        mainRegister.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, Register.class));
            }
        });
    }

}
