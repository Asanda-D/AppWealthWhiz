package vcmsa.projects.wealthwhizap

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import vcmsa.projects.wealthwhizap.databinding.ActivityCategoryExpensesBinding
import vcmsa.projects.wealthwhizap.databinding.ActivityManageCategoriesBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CategoryExpensesActivity : AppCompatActivity(), ExpenseAdapter.OnImageClickListener,
    ExpenseAdapter.OnItemClickListener {

    private lateinit var binding: ActivityCategoryExpensesBinding
    private lateinit var totalAmountTextView: TextView
    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var dateRangeTextView: TextView
    private lateinit var categoryNameTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences

    private var startDate: String = ""
    private var endDate: String = ""
    private var categoryId: String = ""
    private var categoryName: String = ""
    private val firebaseManager = FirebaseManager(this)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val randFormat: NumberFormat = NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = 2
        currency = java.util.Currency.getInstance("ZAR")
    }

    private var expensesList: List<Expense> = emptyList()
    private var categoriesMap: Map<String, CategoryEntity> = emptyMap()
    private lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitleTextColor(Color.parseColor("#000D87"))
        supportActionBar?.title = "  \t\t\tCATEGORY EXPENSES"

        sharedPreferences = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)

        // Get category ID and name from intent
        categoryId = intent.getStringExtra("CATEGORY_ID") ?: ""
        categoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""

        if (categoryId.isEmpty()) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        totalAmountTextView = findViewById(R.id.totalAmountTextView)
        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)
        dateRangeTextView = findViewById(R.id.dateRangeTextView)
        categoryNameTextView = findViewById(R.id.categoryNameTextView)

        // Set category name
        categoryNameTextView.text = categoryName

        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(this, expensesList, categoriesMap.toMutableMap())
        expenseAdapter.setOnImageClickListener(this)
        expenseAdapter.setOnItemClickListener(this)
        expensesRecyclerView.adapter = expenseAdapter

        // Set default date range to current month
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDate = dateFormat.format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        endDate = dateFormat.format(calendar.time)
        updateDateRangeText()

        loadCategories()
        loadExpenses()

        // Setup date range picker
        dateRangeTextView.setOnClickListener {
            showDateRangePicker()
        }
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
        loadExpenses()
    }

    private fun getCurrentUserUsername(): String {
        return sharedPreferences.getString("loggedInUsername", "") ?: ""
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val username = getCurrentUserUsername()
                if (username.isEmpty()) {
                    Toast.makeText(this@CategoryExpensesActivity, "User not logged in", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                firebaseManager.getCategories(username).fold(
                    onSuccess = { categories ->
                        categoriesMap = categories.associateBy { it.id }
                        expenseAdapter.updateCategories(categoriesMap)
                    },
                    onFailure = { e ->
                        Toast.makeText(this@CategoryExpensesActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@CategoryExpensesActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            try {
                val username = getCurrentUserUsername()
                if (username.isEmpty()) {
                    Toast.makeText(this@CategoryExpensesActivity, "User not logged in", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                firebaseManager.getExpensesByCategoryAndDateRange(
                    username = username,
                    categoryId = categoryId,
                    startDate = startDate,
                    endDate = endDate
                ).fold(
                    onSuccess = { expenses ->
                        expensesList = expenses
                        expenseAdapter.updateExpenses(expensesList)
                        val total = expenses.sumOf { it.amount }
                        totalAmountTextView.text = "Total: ${randFormat.format(total)}"
                    },
                    onFailure = { e ->
                        Toast.makeText(this@CategoryExpensesActivity, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@CategoryExpensesActivity, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            startDate = dateFormat.format(Date(selection.first))
            endDate = dateFormat.format(Date(selection.second))
            updateDateRangeText()
            loadExpenses()
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun updateDateRangeText() {
        dateRangeTextView.text = "$startDate to $endDate"
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
        lifecycleScope.launch {
            try {
                firebaseManager.deleteExpense(expense.id.toString()).fold(
                    onSuccess = {
                        Toast.makeText(this@CategoryExpensesActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                        loadExpenses()
                    },
                    onFailure = { e ->
                        Toast.makeText(this@CategoryExpensesActivity, "Error deleting expense: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@CategoryExpensesActivity, "Error deleting expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}