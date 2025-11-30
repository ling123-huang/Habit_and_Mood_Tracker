package com.example.habitmood

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyPage : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvTotalHabits: TextView
    private lateinit var tvCurrentStreak: TextView
    private lateinit var tvCompletionRate: TextView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // View 초기화
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnLogout = findViewById(R.id.btnLogout)
        bottomNavigationView = findViewById(R.id.bottomAppBar)

        // 사용자 정보 로드
        loadUserData()

        // 프로필 수정 버튼
        btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Edit Profile", Toast.LENGTH_SHORT).show()
        }

        // 로그아웃 버튼
        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 설정 항목 클릭 리스너들
        findViewById<android.view.View>(R.id.layoutNotifications).setOnClickListener {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.layoutTheme).setOnClickListener {
            Toast.makeText(this, "Theme", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.layoutBackup).setOnClickListener {
            Toast.makeText(this, "Backup & Restore", Toast.LENGTH_SHORT).show()
        }

        // 하단 네비게이션 설정
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // 현재 페이지를 MY로 설정
        bottomNavigationView.selectedItemId = R.id.menu_profile

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_stats -> {
                    // Mood 화면으로 이동
                    // TODO: MoodActivity 생성 후 연결
                    true
                }
                R.id.home_page -> {
                    // Home 화면으로 이동
                    val intent = Intent(this, Home::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.menu_profile -> {
                    // 현재 MY 화면이므로 아무것도 안 함
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            tvUserName.text = user.displayName ?: "User"
            tvUserEmail.text = user.email ?: "user@example.com"
        } else {
            tvUserName.text = "Guest"
            tvUserEmail.text = "Not logged in"
        }
    }

    override fun onResume() {
        super.onResume()
        // 다른 화면에서 돌아왔을 때 MY 선택 상태 유지
        bottomNavigationView.selectedItemId = R.id.menu_profile
    }
}