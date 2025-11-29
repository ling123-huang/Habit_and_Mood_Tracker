package com.example.habitmood

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class Home: AppCompatActivity() {

    private lateinit var habitRecyclerView: RecyclerView
    private lateinit var fabAddHabit: FloatingActionButton
    private lateinit var habitAdapter: HabitAdapter
    private lateinit var tvDate: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    // 초기 습관 목록(일시적)
    private val habitList = mutableListOf<Habit>()


    companion object {
        private const val REQUEST_ADD_HABIT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        habitRecyclerView = findViewById(R.id.habit_RecyclerView)
        fabAddHabit = findViewById(R.id.fabAddHabit)
        tvDate = findViewById(R.id.tvDate)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()


        setCurrentDate()

        habitAdapter = HabitAdapter(habitList) { position ->
            if (position !in habitList.indices) return@HabitAdapter

            val user = auth.currentUser ?: return@HabitAdapter
            val habit = habitList[position]

            db.collection("users")
                .document(user.uid)
                .collection("habits")
                .document(habit.id)
                .delete()
                .addOnSuccessListener {
                    habitList.removeAt(position)
                    habitAdapter.notifyItemRemoved(position)
                }
        }
        habitRecyclerView.layoutManager = LinearLayoutManager(this)
        habitRecyclerView.adapter = habitAdapter

        fabAddHabit.setOnClickListener {
            // AddHabitActivity로 이동
            val intent = Intent(this, AddHabitActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_HABIT)
        }
        loadHabitsFromFirestore()
    }

    private fun setCurrentDate() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMM", Locale.ENGLISH)
        val currentDate = dateFormat.format(calendar.time)
        tvDate.text = currentDate
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ADD_HABIT && resultCode == Activity.RESULT_OK) {

            val newHabitName = data?.getStringExtra("NEW_HABIT_NAME") ?: return
            val user = auth.currentUser ?: return

            val habitData = hashMapOf(
                "name" to newHabitName,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("users")
                .document(user.uid)
                .collection("habits")
                .add(habitData)
                .addOnSuccessListener { docRef ->
                    val habit = Habit(docRef.id, newHabitName)
                    habitList.add(habit)
                    habitAdapter.notifyItemInserted(habitList.size - 1)
                }
        }

    }
    private fun loadHabitsFromFirestore() {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .collection("habits")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { snapshot ->
                habitList.clear()
                for (doc in snapshot) {
                    val name = doc.getString("name") ?: ""
                    val habit = Habit(id = doc.id, name = name)
                    habitList.add(habit)
                }
                habitAdapter.notifyDataSetChanged()
            }
    }

}


