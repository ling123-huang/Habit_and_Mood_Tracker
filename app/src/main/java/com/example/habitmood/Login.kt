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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.FirebaseNetworkException


class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, Home::class.java))
            finish()
            return
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

            auth.signInWithEmailAndPassword(inputEmail, inputPassword)
                .addOnSuccessListener {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Home::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    val msg = when (e) {
                        is FirebaseAuthInvalidUserException -> "No account found for this email"
                        is FirebaseAuthInvalidCredentialsException -> "Wrong password or invalid email"
                        is FirebaseNetworkException -> "Network error, please check your connection"
                        else -> "Login failed: ${e.localizedMessage}"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
        }


        val signupBtn = findViewById<Button>(R.id.signupButton)
        signupBtn.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

    }
}