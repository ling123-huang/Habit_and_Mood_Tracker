package com.example.habitmood

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Home: AppCompatActivity() {

    private lateinit var habitRecyclerView: RecyclerView
    private lateinit var fabAddHabit: FloatingActionButton
    private lateinit var habitAdapter: HabitAdapter
    private lateinit var tvDate: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var selectedMood: Int? = null

    private lateinit var btnMoodBad: TextView
    private lateinit var btnMoodSad: TextView
    private lateinit var btnMoodNatural: TextView
    private lateinit var btnMoodGood: TextView
    private lateinit var btnMoodVeryGood: TextView
    private lateinit var tvCurrentMood: TextView
    private lateinit var etMoodNote: TextInputEditText
    private lateinit var layoutMoodNote: TextInputLayout
    private lateinit var btnReset: TextView
    private lateinit var btnSaveMood: MaterialButton


    private val habitList = mutableListOf<Habit>()

    companion object {
        private const val REQUEST_ADD_HABIT = 1001
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }

        }

        habitRecyclerView = findViewById(R.id.habit_RecyclerView)
        fabAddHabit = findViewById(R.id.fabAddHabit)
        tvDate = findViewById(R.id.tvDate)
        tvTotalCount = findViewById(R.id.habit_count)
        tvUserName = findViewById(R.id.tvUserName)
        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomAppBar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnMoodBad = findViewById(R.id.btnMoodBad)
        btnMoodSad = findViewById(R.id.btnMoodSad)
        btnMoodNatural = findViewById(R.id.btnMoodNatural)
        btnMoodGood = findViewById(R.id.btnMoodGood)
        btnMoodVeryGood = findViewById(R.id.btnMoodVeryGood)
        tvCurrentMood = findViewById(R.id.tvCurrentMood)
        etMoodNote = findViewById(R.id.etMoodNote)
        layoutMoodNote = findViewById(R.id.layoutMoodNote)
        btnSaveMood = findViewById(R.id.btnSaveMood)
        btnReset = findViewById(R.id.btnReset)

        btnReset.paintFlags = btnReset.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG

        btnReset.setOnClickListener {
            resetMoodSelection()
        }
        habitAdapter = HabitAdapter(habitList) { position ->
            val habit = habitList.getOrNull(position) ?: return@HabitAdapter
            val user = auth.currentUser ?: return@HabitAdapter

            val habitRef = db.collection("users")
                .document(user.uid)
                .collection("habits")
                .document(habit.id)

            habitRef.collection("checkins")
                .get()
                .addOnSuccessListener { checkinsSnapshot ->
                    val batch = db.batch()
                    for (doc in checkinsSnapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.delete(habitRef)

                    batch.commit()
                        .addOnSuccessListener {
                            habitList.removeAt(position)
                            habitAdapter.notifyItemRemoved(position)
                            updateTotalCount()

                            Toast.makeText(
                                this,
                                "Habit and all check-ins deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Failed to delete habit: ${e.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to load check-ins: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        habitRecyclerView.layoutManager = LinearLayoutManager(this)
        habitRecyclerView.adapter = habitAdapter

        fabAddHabit.setOnClickListener {
            val intent = Intent(this, AddHabitActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_HABIT)
        }
        setupBottomNavigation()
        setCurrentDate()
        setUserName()

        btnMoodBad.setOnClickListener {
            selectedMood = 1
            updateMoodUI()
        }
        btnMoodSad.setOnClickListener {
            selectedMood = 2
            updateMoodUI()
        }
        btnMoodNatural.setOnClickListener {
            selectedMood = 3
            updateMoodUI()
        }
        btnMoodGood.setOnClickListener {
            selectedMood = 4
            updateMoodUI()
        }
        btnMoodVeryGood.setOnClickListener {
            selectedMood = 5
            updateMoodUI()
        }

        btnSaveMood.setOnClickListener {
            saveTodayMood()
        }

        loadHabitsFromFirestore()
        loadTodayMood()
    }
    private fun updateMoodUI() {
        val moods = listOf(
            btnMoodBad to 1,
            btnMoodSad to 2,
            btnMoodNatural to 3,
            btnMoodGood to 4,
            btnMoodVeryGood to 5
        )

        moods.forEach { (view, moodValue) ->
            if (selectedMood == moodValue) {
                view.alpha = 1.0f
                view.scaleX = 1.2f
                view.scaleY = 1.2f
            } else {
                view.alpha = 0.4f
                view.scaleX = 1.0f
                view.scaleY = 1.0f
            }
        }

        tvCurrentMood.text = when (selectedMood) {
            1 -> "Selected: 😭"
            2 -> "Selected: 😥"
            3 -> "Selected: 😐"
            4 -> "Selected: 😄"
            5 -> "Selected: 🤩"
            else -> getString(R.string.select_a_mood)
        }
    }

    private fun saveTodayMood() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val mood = selectedMood
        val note = etMoodNote.text?.toString() ?: ""

        if (mood == null) {
            Toast.makeText(this, "Please select a mood", Toast.LENGTH_SHORT).show()
            return
        }

        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val data = hashMapOf(
            "date" to todayKey,
            "mood" to mood,
            "note" to note,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(user.uid)
            .collection("moods")
            .document(todayKey)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Mood saved!", Toast.LENGTH_SHORT).show()

                etMoodNote.clearFocus()
                layoutMoodNote.visibility = View.GONE
                btnSaveMood.visibility = View.GONE
            }
    }

    private fun setUserName() {
        val user = auth.currentUser
        val name = user?.displayName ?: "User"
        tvUserName.text = "Hello, $name 👋"
    }

    private fun setupBottomNavigation() {
        // Set the current page as Home
        bottomNavigationView.selectedItemId = R.id.home_page

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_stats -> {
                    val intent = Intent(this, MoodStatistics::class.java)
                    startActivity(intent)
                    true
                }
                R.id.home_page -> {
                    true
                }
                R.id.menu_profile -> {
                    val intent = Intent(this, MyPage::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
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

            // Also get alarm-related information
            val isAlarmOn = data.getBooleanExtra("IS_ALARM_ON", false)
            val alarmHour = if (isAlarmOn) data.getIntExtra("ALARM_HOUR", 9) else null
            val alarmMinute = if (isAlarmOn) data.getIntExtra("ALARM_MINUTE", 0) else null
            val selectedDays =
                data.getStringArrayListExtra("SELECTED_DAYS") ?: arrayListOf<String>()

            // Data map to store in Firestore
            val habitData = hashMapOf(
                "name" to newHabitName,
                "createdAt" to FieldValue.serverTimestamp(),
                "isAlarmOn" to isAlarmOn,
                "selectedDays" to selectedDays
            )

            // If null, omit the field; if present, save it.
            alarmHour?.let { habitData["alarmHour"] = it }
            alarmMinute?.let { habitData["alarmMinute"] = it }

            db.collection("users")
                .document(user.uid)
                .collection("habits")
                .add(habitData)
                .addOnSuccessListener {
                    loadHabitsFromFirestore()
                }
        }
    }
    private fun updateTotalCount() {
        val realCount = habitList.count { it.id != "DIVIDER" }
        tvTotalCount.text = "Total: $realCount"
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
                val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Temporary list: Habit data including check status
                val tempHabits = mutableListOf<Pair<Habit, Boolean>>()

                for (doc in snapshot) {
                    val name = doc.getString("name") ?: ""
                    val isAlarmOn = doc.getBoolean("isAlarmOn") ?: false
                    val alarmHour = doc.getLong("alarmHour")?.toInt()
                    val alarmMinute = doc.getLong("alarmMinute")?.toInt()
                    val selectedDays =
                        (doc.get("selectedDays") as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList()
                    val createdAtTs = doc.getTimestamp("createdAt")
                    val createdDate = createdAtTs?.let { ts ->
                        dateFormat.format(ts.toDate())
                    } ?: ""

                    val habit = Habit(
                        id = doc.id,
                        name = name,
                        isAlarmOn = isAlarmOn,
                        alarmHour = alarmHour,
                        alarmMinute = alarmMinute,
                        selectedDays = selectedDays,
                        createdDate = createdDate
                    )

                    if (isAlarmOn && alarmHour != null && alarmMinute != null) {
                        scheduleHabitReminder(
                            habit.id,
                            habit.name,
                            alarmHour,
                            alarmMinute,
                            selectedDays,
                            habit.createdDate
                        )
                    }

                    // Temporary save for checking today
                    tempHabits.add(Pair(habit, false))
                }

                //Check today's status for each habit
                var completedCount = 0
                tempHabits.forEachIndexed { index, (habit, _) ->
                    db.collection("users")
                        .document(user.uid)
                        .collection("habits")
                        .document(habit.id)
                        .collection("checkins")
                        .document(todayKey)
                        .get()
                        .addOnSuccessListener { doc ->
                            val isChecked = doc.exists()
                            tempHabits[index] = Pair(habit, isChecked)

                            completedCount++

                            // Once you have checked the status of all habits, sort them.
                            if (completedCount == tempHabits.size) {
                                // Uncheck habits first, check habits later
                                val unchecked = tempHabits.filter { !it.second }.map { it.first }
                                val checked = tempHabits.filter { it.second }.map { it.first }

                                habitList.clear()
                                habitList.addAll(unchecked)

                                //Add a divider (only when there are checked habits)
                                if (checked.isNotEmpty()) {
                                    habitList.add(Habit(id = "DIVIDER", name = "TODAY COMPLETE"))
                                    habitList.addAll(checked)
                                }

                                habitAdapter.notifyDataSetChanged()
                                updateTotalCount()
                            }
                        }
                }

                // Update immediately if there are no habits
                if (tempHabits.isEmpty()) {
                    habitAdapter.notifyDataSetChanged()
                    updateTotalCount()
                }
            }
    }

    private fun scheduleHabitReminder(
        habitId: String,
        habitName: String,
        hour: Int,
        minute: Int,
        selectedDays: List<String>,
        habitCreatedDate: String
    ) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("HABIT_ID", habitId)
            putExtra("HABIT_NAME", habitName)
            putStringArrayListExtra("SELECTED_DAYS", ArrayList(selectedDays))
            putExtra("HABIT_MESSAGE", "Time for \"$habitName\" 🙌")
            putExtra("HABIT_CREATED_DATE", habitCreatedDate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.cancel(pendingIntent)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }



    override fun onResume() {
        super.onResume()
        loadHabitsFromFirestore()
    }

    private fun loadTodayMood() {
        val user = auth.currentUser ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("users")
            .document(user.uid)
            .collection("moods")
            .document(todayKey)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val savedMood = document.getLong("mood")?.toInt()

                    if (savedMood != null && savedMood in 1..5) {
                        selectedMood = savedMood
                        updateMoodUI()
                    }
                    layoutMoodNote.visibility = View.GONE
                    btnSaveMood.visibility = View.GONE
                }
            }
    }

    // Function to Reset Selection and Input
    private fun resetMoodSelection() {
        val user = auth.currentUser ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Request to delete today's records in Firestore
        db.collection("users")
            .document(user.uid)
            .collection("moods")
            .document(todayKey)
            .delete()
            .addOnSuccessListener {
                selectedMood = null

                etMoodNote.text?.clear()
                etMoodNote.clearFocus()

                updateMoodUI()
                //Shows the input field and save button so that you can enter again
                layoutMoodNote.visibility = View.VISIBLE
                btnSaveMood.visibility = View.VISIBLE

                Toast.makeText(this, "Mood deleted and reset", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Error message when deletion fails
                Toast.makeText(this, "Failed to delete mood", Toast.LENGTH_SHORT).show()
            }
    }
}
