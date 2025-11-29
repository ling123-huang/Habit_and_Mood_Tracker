package com.example.habitmood

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText

class SignUp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val nameField = findViewById<EditText>(R.id.nameField)
        val emailField = findViewById<EditText>(R.id.emailFieldSignUp)
        val passwordField = findViewById<EditText>(R.id.passwordFieldSignUp)
        val confirmPasswordField = findViewById<EditText>(R.id.confirmPasswordField)


        btnBack.setOnClickListener {
            finish() // 현재 화면 종료 -> 로그인 화면으로 복귀
        }

        btnRegister.setOnClickListener {
            val name = nameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()
            val confirmPassword = confirmPasswordField.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            prefs.edit()
                .putString("name", name)
                .putString("email", email)
                .putString("password", password)
                .apply()
            // 회원가입 로직 처리 (Firebase 등 연동 시 이곳에 작성)
            Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()
            finish() // 가입 후 로그인 화면으로 돌아가기
        }

        tvGoToLogin.setOnClickListener {
            finish() // 로그인 화면으로 돌아가기
        }
    }
}