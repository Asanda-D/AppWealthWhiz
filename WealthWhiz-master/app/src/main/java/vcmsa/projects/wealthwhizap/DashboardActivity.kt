package vcmsa.projects.wealthwhizap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.view.View

//All Referencing is available on GROUP_5_PROG7313_POE_PART_3 pdf

class DashboardActivity : AppCompatActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var imgCoin: ImageView
    private lateinit var imgSettings: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var txtCoins: TextView
    private lateinit var txtMonthlyBudget: TextView
    private lateinit var txtLeftToSpend: TextView
    private lateinit var txtBudgetStatus: TextView
    private lateinit var txtBudgetStatusValue: TextView
    private lateinit var progressBarBudget: ProgressBar
    private lateinit var textViewProgress: TextView

    private lateinit var imgCategories: ImageView
    private lateinit var imgSetGoal: ImageView
    private lateinit var imgBreakdown: ImageView
    private lateinit var imgExpenses: ImageView
    private lateinit var imgUser: ImageView
    private lateinit var imgAddExpense: ImageView
    private lateinit var imgBadgeIcon: ImageView

    // New: streak and badge views
    private lateinit var txtStreak: TextView

    private val firebaseManager = FirebaseManager(this)

    // South African Rand currency formatter
    private val randFormat: NumberFormat = NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = 2
        currency = java.util.Currency.getInstance("ZAR")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)



        val currentStreakTextView = findViewById<TextView>(R.id.currentStreak)

        lifecycleScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val result = FirebaseManager.getCurrentStreak(uid)
                result.onSuccess { streak ->
                    currentStreakTextView.text = "$streak Day Streak"
                }.onFailure {
                    currentStreakTextView.text = "0 Day Streak"  // fallback
                }
            }
        }


        // Top bar
        imgProfile = findViewById(R.id.image)
        txtUsername = findViewById(R.id.user)
        imgSettings = findViewById(R.id.imageView9)

        // Budget fields
        txtMonthlyBudget = findViewById(R.id.textViewMonthlyBudget)
        txtLeftToSpend = findViewById(R.id.textViewLeftToSpend)
        txtBudgetStatus = findViewById(R.id.budget_status_title)
        txtBudgetStatusValue = findViewById(R.id.budget_status_value)
        progressBarBudget = findViewById(R.id.progressBarBudget)
        textViewProgress = findViewById(R.id.textViewProgress)

        // Bottom navigation icons
        imgCategories = findViewById(R.id.imageView11)
        imgSetGoal = findViewById(R.id.imageView12)
        imgBreakdown = findViewById(R.id.imageView13)
        imgExpenses = findViewById(R.id.imageView14)
        imgUser = findViewById(R.id.imageView15)
        imgAddExpense = findViewById(R.id.imageView)
        imgBadgeIcon = findViewById(R.id.badges_icon)

        // New: bind streak 
        txtStreak = findViewById(R.id.currentStreak)

        // Set username from SharedPreferences
        val sharedPref = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("loggedInUsername", "")

        if (username.isNullOrEmpty()) {
            // If no username is found, redirect to login
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        txtUsername.text = username

        // Update budget and expenses
        updateBudgetAndExpenses()

        // Setup click listeners
        imgCategories.setOnClickListener {
            startActivity(Intent(this, ManageCategoriesActivity::class.java))
        }
        imgSetGoal.setOnClickListener {
            startActivity(Intent(this, GoalActivity::class.java))
        }
        imgBreakdown.setOnClickListener {
            startActivity(Intent(this, MonthlyBreakdownActivity::class.java))
        }
        imgExpenses.setOnClickListener {
            startActivity(Intent(this, AllExpensesActivity::class.java))
        }
        imgUser.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        imgAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
        imgBadgeIcon.setOnClickListener {
            startActivity(Intent(this, BadgesActivity::class.java))
        }
        imgSettings.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateBudgetAndExpenses()

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid

        lifecycleScope.launch {
            // Update the login streak
            firebaseManager.updateLoginStreakOnAppOpen(uid).fold(
                onSuccess = { newStreak ->
                    txtStreak.text = "$newStreak Day Streak"
                },
                onFailure = { e ->
                    Log.e("DashboardActivity", "Streak update failed: ${e.message}")
                }
            )

            // Optionally fetch and re-confirm the current streak
            firebaseManager.getCurrentStreak(uid).fold(
                onSuccess = { streak ->
                    txtStreak.text = "$streak Day Streak"
                },
                onFailure = {
                    Log.e("DashboardActivity", "Failed to get streak: ${it.message}")
                }
            )
        }
    }


    private fun updateBudgetAndExpenses() {
        lifecycleScope.launch {
            try {
                val username = getCurrentUserUsername()
                val calendar = Calendar.getInstance()
                val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

                // Debug logging
                Log.d("DashboardActivity", "Current month: $currentMonth")
                Log.d("DashboardActivity", "Username: $username")

                // Get current month's budget from Firebase
                firebaseManager.getGoalByMonth(username, currentMonth).fold(
                    onSuccess = { goal ->
                        val monthlyBudget = goal?.monthlyBudget ?: 0.0
                        Log.d("DashboardActivity", "Monthly budget: $monthlyBudget")

                        // Get current month's expenses from Firebase
                        firebaseManager.getExpenses(username).fold(
                            onSuccess = { expenses ->
                                Log.d("DashboardActivity", "Total expenses found: ${expenses.size}")

                                // Filter expenses for current month
                                val currentMonthExpenses = expenses.filter { expense ->
                                    try {
                                        val expenseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            .parse(expense.dateTime.split(" ")[0]) ?: return@filter false
                                        val expenseMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                            .format(expenseDate)
                                        Log.d("DashboardActivity", "Checking expense date: ${expense.dateTime} -> $expenseMonth")
                                        expenseMonth == currentMonth
                                    } catch (e: Exception) {
                                        Log.e("DashboardActivity", "Error parsing expense date: ${e.message}")
                                        false
                                    }
                                }

                                Log.d("DashboardActivity", "Current month expenses: ${currentMonthExpenses.size}")
                                val totalExpenses = currentMonthExpenses.sumOf { it.amount }
                                val leftToSpend = monthlyBudget - totalExpenses
                                val progress = if (monthlyBudget > 0) {
                                    val percentage = (totalExpenses / monthlyBudget * 100).toInt()
                                    if (percentage > 100) 100 else percentage
                                } else 0

                                Log.d("DashboardActivity", "Total expenses: $totalExpenses")
                                Log.d("DashboardActivity", "Left to spend: $leftToSpend")
                                Log.d("DashboardActivity", "Progress: $progress%")

                                runOnUiThread {
                                    txtMonthlyBudget.text = randFormat.format(monthlyBudget)
                                    txtLeftToSpend.text = randFormat.format(leftToSpend)
                                    progressBarBudget.progress = progress
                                    textViewProgress.text = "$progress%"

                                    // Update budget status with more precise calculations
                                    when {
                                        monthlyBudget == 0.0 -> {
                                            txtBudgetStatus.text = "No Budget Set"
                                            txtBudgetStatusValue.text = randFormat.format(totalExpenses)
                                            txtBudgetStatusValue.setTextColor(resources.getColor(R.color.budget_over, theme))
                                        }
                                        totalExpenses > monthlyBudget -> {
                                            val overBudget = totalExpenses - monthlyBudget
                                            txtBudgetStatus.text = "Over Budget"
                                            txtBudgetStatusValue.text = randFormat.format(overBudget)
                                            txtBudgetStatusValue.setTextColor(resources.getColor(R.color.budget_over, theme))
                                        }
                                        totalExpenses == monthlyBudget -> {
                                            txtBudgetStatus.text = "On Budget"
                                            txtBudgetStatusValue.text = "0.00"
                                            txtBudgetStatusValue.setTextColor(resources.getColor(R.color.budget_good, theme))
                                        }
                                        else -> {
                                            val underBudget = monthlyBudget - totalExpenses
                                            txtBudgetStatus.text = "Under Budget"
                                            txtBudgetStatusValue.text = randFormat.format(underBudget)
                                            txtBudgetStatusValue.setTextColor(resources.getColor(R.color.budget_good, theme))
                                        }
                                    }
                                }
                            },
                            onFailure = { e ->
                                Log.e("DashboardActivity", "Error getting expenses: ${e.message}")
                                runOnUiThread {
                                    Toast.makeText(this@DashboardActivity, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                    onFailure = { e ->
                        Log.e("DashboardActivity", "Error getting goal: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this@DashboardActivity, "Error loading budget: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error in updateBudgetAndExpenses: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@DashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getCurrentUserUsername(): String {
        val sharedPref = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
        return sharedPref.getString("loggedInUsername", "") ?: ""
    }
}

