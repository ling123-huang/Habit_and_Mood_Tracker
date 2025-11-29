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
import android.widget.Toast


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
            val inputEmail = emailField.text.toString().trim()
            val inputPassword = passwordField.text.toString()

            if (inputEmail.isEmpty() || inputPassword.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val savedEmail = prefs.getString("email", null)
            val savedPassword = prefs.getString("password", null)

            if (savedEmail == null || savedPassword == null) {
                Toast.makeText(this, "No account found. Please sign up first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (inputEmail == savedEmail && inputPassword == savedPassword) {
                val intent = Intent(this, Home::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Wrong email or password", Toast.LENGTH_SHORT).show()
            }
        }

        val signupBtn = findViewById<Button>(R.id.signupButton)
        signupBtn.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

    }
}