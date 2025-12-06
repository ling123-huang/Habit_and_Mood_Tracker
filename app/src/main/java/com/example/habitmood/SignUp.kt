package com.example.habitmood

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class SignUp : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
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
        auth = FirebaseAuth.getInstance()



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

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        user.updateProfile(profileUpdate)
                    }

                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    val msg = when (e) {
                        is FirebaseAuthWeakPasswordException -> "Password too weak (min 6 chars recommended)"
                        is FirebaseAuthUserCollisionException -> "This email is already registered"
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email format"
                        else -> "Sign up failed: ${e.localizedMessage}"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }
}