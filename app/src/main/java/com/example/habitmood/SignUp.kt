package com.example.habitmood

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignUp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        btnBack.setOnClickListener {
            finish() // 현재 화면 종료 -> 로그인 화면으로 복귀
        }

        btnRegister.setOnClickListener {
            // 회원가입 로직 처리 (Firebase 등 연동 시 이곳에 작성)
            Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()
            finish() // 가입 후 로그인 화면으로 돌아가기
        }

        tvGoToLogin.setOnClickListener {
            finish() // 로그인 화면으로 돌아가기
        }
    }
}