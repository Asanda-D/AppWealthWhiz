package vcmsa.projects.wealthwhizap

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import vcmsa.projects.wealthwhizap.databinding.ActivityGoalBinding
import vcmsa.projects.wealthwhizap.databinding.ActivityManageCategoriesBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GoalActivity : AppCompatActivity(), GoalAdapter.OnGoalClickListener {

    private lateinit var binding: ActivityGoalBinding
    private lateinit var spinnerMonth: Spinner
    private lateinit var editTextMinGoal: EditText
    private lateinit var editTextMaxGoal: EditText
    private lateinit var buttonSave: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var goalAdapter: GoalAdapter
    private lateinit var instructionsText: TextView
    private lateinit var editTextBudget: EditText
    private lateinit var textViewCurrentYear: TextView
    private val firebaseManager = FirebaseManager()

    private val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // South African Rand currency formatter
    private val randFormat: NumberFormat = NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = 2
        currency = java.util.Currency.getInstance("ZAR")
    }

    private fun getCurrentMonthYear(): String {
        val calendar = Calendar.getInstance()
        val month = months[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        return "$month $year"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitleTextColor(Color.parseColor("#000D87"))
        supportActionBar?.title = " \t\t\tSET BUDGET GOAL"

        // Initialize views
        spinnerMonth = findViewById(R.id.spinnerMonth)
        editTextMinGoal = findViewById(R.id.editTextMinGoal)
        editTextMaxGoal = findViewById(R.id.editTextMaxGoal)
        buttonSave = findViewById(R.id.buttonSave)
        recyclerView = findViewById(R.id.recyclerViewGoals)
        instructionsText = findViewById(R.id.textInstructions)
        editTextBudget = findViewById(R.id.editTextBudget)
        textViewCurrentYear = findViewById(R.id.textViewCurrentYear)

        // Set current year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        textViewCurrentYear.text = "Year: $currentYear"

        // Set instructions text
        instructionsText.text = "Tap a goal to edit | Long press to delete"

        // Set up the spinner with months
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = adapter

        // Set up RecyclerView with click listener
        goalAdapter = GoalAdapter(emptyList())
        goalAdapter.setOnGoalClickListener(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = goalAdapter

        // Save button functionality
        buttonSave.setOnClickListener {
            saveGoals()
        }

        // Load goals
        loadGoals()
    }

    override fun onResume() {
        super.onResume()
        loadGoals() // Reload goals when activity resumes
    }

    private fun loadGoals() {
        lifecycleScope.launch {
            try {
                val username = getCurrentUserUsername()
                Log.d("GoalActivity", "Loading goals for username: $username")
                firebaseManager.getGoals(username).fold(
                    onSuccess = { goals ->
                        Log.d("GoalActivity", "Loaded ${goals.size} goals")
                        goalAdapter.updateGoals(goals)
                        if (goals.isEmpty()) {
                            Toast.makeText(this@GoalActivity, "No goals found. Add your first goal!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { e ->
                        Log.e("GoalActivity", "Error loading goals: ${e.message}")
                        Toast.makeText(this@GoalActivity, "Error loading goals: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("GoalActivity", "Error in loadGoals: ${e.message}")
                Toast.makeText(this@GoalActivity, "Error loading goals: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onGoalClick(goal: Goal) {
        // When a goal is clicked, load it into the form for editing
        val monthName = goal.month.split(" ")[0] // Extract just the month name
        val monthIndex = months.indexOf(monthName)
        if (monthIndex != -1) {
            spinnerMonth.setSelection(monthIndex)
        }
        editTextMinGoal.setText(goal.minGoal.toString())
        editTextMaxGoal.setText(goal.maxGoal.toString())
        editTextBudget.setText(goal.monthlyBudget.toString())

        Toast.makeText(this, "Editing ${goal.month}'s goal", Toast.LENGTH_SHORT).show()
    }

    override fun onGoalDelete(goal: Goal) {
        AlertDialog.Builder(this)
            .setTitle("Delete Goal")
            .setMessage("Delete the budget goal for ${goal.month}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    firebaseManager.deleteGoal(goal.id).fold(
                        onSuccess = {
                            Toast.makeText(
                                this@GoalActivity,
                                "${goal.month} goal deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                            clearForm()
                            loadGoals()
                        },
                        onFailure = { e ->
                            Toast.makeText(
                                this@GoalActivity,
                                "Error deleting goal: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveGoals() {
        val monthName = spinnerMonth.selectedItem.toString()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val month = "$monthName $currentYear"
        val minGoal = editTextMinGoal.text.toString().toDoubleOrNull() ?: 0.0
        val maxGoal = editTextMaxGoal.text.toString().toDoubleOrNull() ?: 0.0
        val monthlyBudget = editTextBudget.text.toString().toDoubleOrNull() ?: 0.0
        val username = getCurrentUserUsername()

        Log.d("GoalActivity", "Saving goal for month: $month, username: $username")

        if (month.isNotEmpty() && minGoal > 0 && maxGoal > 0 && monthlyBudget > 0) {
            if (maxGoal < minGoal) {
                Toast.makeText(this, "Max goal must be greater than min", Toast.LENGTH_SHORT).show()
                return
            }
            if (maxGoal > monthlyBudget || minGoal > monthlyBudget) {
                Toast.makeText(this, "Goals cannot exceed the monthly budget", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            lifecycleScope.launch {
                try {
                    val existingGoal = firebaseManager.getGoalByMonth(username, month).getOrNull()
                    Log.d("GoalActivity", "Existing goal found: ${existingGoal != null}")

                    if (existingGoal == null) {
                        // Create new goal
                        val newGoal = Goal(
                            id = "", // Empty ID for new goals, Firestore will generate one
                            userId = username,
                            month = month,
                            minGoal = minGoal,
                            maxGoal = maxGoal,
                            monthlyBudget = monthlyBudget
                        )
                        firebaseManager.saveGoal(newGoal).fold(
                            onSuccess = { id ->
                                Log.d("GoalActivity", "Goal saved successfully with ID: $id")
                                Toast.makeText(
                                    this@GoalActivity,
                                    "Goal saved for $month",
                                    Toast.LENGTH_SHORT
                                ).show()
                                clearForm()
                                loadGoals() // Reload goals after saving
                            },
                            onFailure = { e ->
                                Log.e("GoalActivity", "Error saving goal: ${e.message}")
                                Toast.makeText(
                                    this@GoalActivity,
                                    "Error saving goal: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    } else {
                        // Update existing goal
                        val updatedGoal = existingGoal.copy(
                            minGoal = minGoal,
                            maxGoal = maxGoal,
                            monthlyBudget = monthlyBudget
                        )
                        firebaseManager.updateGoal(updatedGoal).fold(
                            onSuccess = {
                                Log.d("GoalActivity", "Goal updated successfully")
                                Toast.makeText(this@GoalActivity, "$month goal updated", Toast.LENGTH_SHORT)
                                    .show()
                                clearForm()
                                loadGoals() // Reload goals after updating
                            },
                            onFailure = { e ->
                                Log.e("GoalActivity", "Error updating goal: ${e.message}")
                                Toast.makeText(
                                    this@GoalActivity,
                                    "Error updating goal: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e("GoalActivity", "Error in saveGoals: ${e.message}")
                    Toast.makeText(
                        this@GoalActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(this, "Please fill in all fields with valid numbers", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun clearForm() {
        editTextMinGoal.text.clear()
        editTextMaxGoal.text.clear()
        editTextBudget.text.clear()
        spinnerMonth.setSelection(0)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getCurrentUserUsername(): String {
        val sharedPreferences = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("loggedInUsername", "")
        if (username.isNullOrEmpty()) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return ""
        }
        return username
    }
}