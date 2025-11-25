package com.example.habitmood

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // email_icon resize
        val emailField = findViewById<EditText>(R.id.emailField)
        val emailIcon = ContextCompat.getDrawable(this, R.drawable.email)
        val size = (24 * resources.displayMetrics.density).toInt()
        emailIcon?.setBounds(0, 0, size, size)
        emailField.setCompoundDrawables(emailIcon, null, null, null)
        emailField.compoundDrawablePadding = (15 * resources.displayMetrics.density).toInt()

        //password_icon resize
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val passwordIcon = ContextCompat.getDrawable(this, R.drawable.lock)
        passwordIcon?.setBounds(0, 0, size, size)
        passwordField.setCompoundDrawables(passwordIcon, null, null, null)
        passwordField.compoundDrawablePadding = (15 * resources.displayMetrics.density).toInt()

        val loginBtn = findViewById<Button>(R.id.loginButton)
        loginBtn.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }

        val signupBtn = findViewById<Button>(R.id.signupButton)
        signupBtn.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

    }
}