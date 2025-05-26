package vcmsa.projects.wealthwhizap

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AllExpensesActivity : AppCompatActivity(), ExpenseAdapter.OnImageClickListener,
    ExpenseAdapter.OnItemClickListener {

    private lateinit var tvCurrentMonth: TextView
    private lateinit var btnLast7Days: Button
    private lateinit var btnSortByCost: Button
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var expensesRecyclerView: RecyclerView

    private var expensesList: List<Expense> = emptyList()
    private var categoriesMap: Map<String, CategoryEntity> = emptyMap()
    private lateinit var expenseAdapter: ExpenseAdapter
    private var selectedMonth: String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    private val firebaseManager = FirebaseManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_expenses)

        // Initialize views
        tvCurrentMonth = findViewById(R.id.tvMonth)
        btnLast7Days = findViewById(R.id.btnLast7Days)
        btnSortByCost = findViewById(R.id.btnSortByCost)
        btnPrevMonth = findViewById(R.id.btnPreviousMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        expensesRecyclerView = findViewById(R.id.rvExpenses)

        // Set initial month text
        tvCurrentMonth.text = selectedMonth

        // Setup RecyclerView
        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(this, expensesList, categoriesMap.toMutableMap())
        expenseAdapter.setOnImageClickListener(this)
        expenseAdapter.setOnItemClickListener(this)
        expensesRecyclerView.adapter = expenseAdapter

        // Load data
        loadCategories()
        fetchExpensesForMonth(selectedMonth)

        // Setup click listeners
        btnLast7Days.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val last7Days = dateFormat.format(calendar.time)
            filterExpensesByDate(last7Days)
        }

        btnSortByCost.setOnClickListener {
            sortExpensesByCost()
        }

        btnPrevMonth.setOnClickListener {
            navigateMonth(-1)
        }

        btnNextMonth.setOnClickListener {
            navigateMonth(1)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from other activities
        loadCategories()
        fetchExpensesForMonth(selectedMonth)
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val username = getCurrentUserUsername()
                firebaseManager.getCategories(username).fold(
                    onSuccess = { categories ->
                        categoriesMap = categories.associateBy { it.id }
                        expenseAdapter.updateCategories(categoriesMap)

                        // Verify all expenses have valid categories
                        val invalidExpenses = expensesList.filter { expense ->
                            !categoriesMap.containsKey(expense.categoryId)
                        }

                        if (invalidExpenses.isNotEmpty()) {
                            val defaultCategory = categories.firstOrNull { it.name == "Uncategorized" }
                            if (defaultCategory != null) {
                                invalidExpenses.forEach { expense ->
                                    val updatedExpense = expense.copy(categoryId = defaultCategory.id)
                                    firebaseManager.updateExpense(updatedExpense)
                                }
                                fetchExpensesForMonth(selectedMonth)
                            }
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(this@AllExpensesActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AllExpensesActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onImageClick(imageUri: String) {
        val intent = Intent(this, FullscreenImageActivity::class.java)
        intent.putExtra("imageUri", imageUri)
        startActivity(intent)
    }

    override fun onEditClick(expense: Expense) {
        val intent = Intent(this, AddExpenseActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("EXPENSE_ID", expense.id.toString())
        }
        startActivity(intent)
    }

    override fun onDeleteClick(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        firebaseManager.deleteExpense(expense.id.toString()).fold(
                            onSuccess = {
                                Toast.makeText(applicationContext, "Expense deleted", Toast.LENGTH_SHORT).show()
                                fetchExpensesForMonth(selectedMonth)
                            },
                            onFailure = { e ->
                                Toast.makeText(applicationContext, "Error deleting expense: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } catch (e: Exception) {
                        Toast.makeText(applicationContext, "Error deleting expense: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateMonth(monthDelta: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(selectedMonth) ?: return
        calendar.add(Calendar.MONTH, monthDelta)
        selectedMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        tvCurrentMonth.text = selectedMonth
        fetchExpensesForMonth(selectedMonth)
    }

    private fun fetchExpensesForMonth(month: String) {
        lifecycleScope.launch {
            try {
                val username = getCurrentUserUsername()
                firebaseManager.getExpenses(username).fold(
                    onSuccess = { expenses ->
                        // Sort expenses by date in descending order
                        val sortedExpenses = expenses.sortedByDescending { expense ->
                            try {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    .parse(expense.dateTime)
                                date?.time ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                        }

                        // Filter expenses for the selected month
                        expensesList = sortedExpenses.filter { expense ->
                            try {
                                val expenseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .parse(expense.dateTime.split(" ")[0]) ?: return@filter false
                                val expenseMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                    .format(expenseDate)
                                expenseMonth == month
                            } catch (e: Exception) {
                                false
                            }
                        }

                        // Update the adapter with the filtered and sorted expenses
                        expenseAdapter.updateExpenses(expensesList)

                        // Show a message if no expenses are found
                        if (expensesList.isEmpty()) {
                            Toast.makeText(this@AllExpensesActivity, "No expenses found for $month", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(this@AllExpensesActivity, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AllExpensesActivity, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

    private fun filterExpensesByDate(last7Days: String) {
        lifecycleScope.launch {
            try {
                val username = getCurrentUserUsername()
                firebaseManager.getExpenses(username).fold(
                    onSuccess = { expenses ->
                        expensesList = expenses.filter { expense ->
                            val expenseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expense.dateTime) ?: return@filter false
                            val last7DaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(last7Days) ?: return@filter false
                            !expenseDate.before(last7DaysDate)
                        }
                        expenseAdapter.updateExpenses(expensesList)
                    },
                    onFailure = { e ->
                        Toast.makeText(this@AllExpensesActivity, "Error filtering expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AllExpensesActivity, "Error filtering expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sortExpensesByCost() {
        expensesList = expensesList.sortedByDescending { it.amount }
        expenseAdapter.updateExpenses(expensesList)
    }
}