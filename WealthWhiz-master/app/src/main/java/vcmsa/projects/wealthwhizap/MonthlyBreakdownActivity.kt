package vcmsa.projects.wealthwhizap

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.launch
import vcmsa.projects.wealthwhizap.databinding.ActivityMonthlyBreakdownBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MonthlyBreakdownActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonthlyBreakdownBinding
    private lateinit var pieChart: PieChart
    private lateinit var tvMonth: TextView
    private lateinit var tvGoalRange: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var legendLayout: LinearLayout

    private var selectedMonth: String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    private val firebaseManager = FirebaseManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthlyBreakdownBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitleTextColor(Color.parseColor("#000D87"))
        supportActionBar?.title = "   \t\t\tWEALTH WRAPPED"

        pieChart = findViewById(R.id.pieChart)
        tvMonth = findViewById(R.id.tvMonth)
        tvGoalRange = findViewById(R.id.tvGoalsInfo)
        tvTotalSpent = findViewById(R.id.tvTotalSpent)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        legendLayout = findViewById(R.id.legendLayout)

        tvMonth.text = selectedMonth

        btnPrevMonth.setOnClickListener { changeMonth(-1) }
        btnNextMonth.setOnClickListener { changeMonth(1) }

        loadDataForMonth(selectedMonth)
    }

    private fun changeMonth(delta: Int) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        calendar.time = sdf.parse(selectedMonth) ?: Date()
        calendar.add(Calendar.MONTH, delta)
        selectedMonth = sdf.format(calendar.time)
        tvMonth.text = selectedMonth
        loadDataForMonth(selectedMonth)
    }

    private fun loadDataForMonth(month: String) {
        lifecycleScope.launch {
            val username = getUsername()
            val expensesResult = firebaseManager.getExpenses(username)
            val categoriesResult = firebaseManager.getCategories(username)
            val goalResult = firebaseManager.getGoalByMonth(username, month)

            if (expensesResult.isFailure || categoriesResult.isFailure) {
                Toast.makeText(this@MonthlyBreakdownActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val allExpenses = expensesResult.getOrNull() ?: emptyList()
            val allCategories = categoriesResult.getOrNull() ?: emptyList()
            val goal = goalResult.getOrNull()

            val filteredExpenses = allExpenses.filter {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val expenseDate = sdf.parse(it.dateTime.split(" ")[0]) ?: return@filter false
                    val expenseMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(expenseDate)
                    expenseMonth == month
                } catch (e: Exception) {
                    false
                }
            }

            val categoryTotals = filteredExpenses.groupBy { it.categoryId }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val totalSpending = categoryTotals.values.sum().coerceAtLeast(1.0)
            tvTotalSpent.text = "Total Spent: R%.2f".format(totalSpending)

            val entries = ArrayList<PieEntry>()
            val colors = ArrayList<Int>()
            val categoryIcons = HashMap<String, Int>()

            categoryTotals.forEach { (categoryId, total) ->
                val category = allCategories.find { it.id == categoryId }
                if (category != null && total > 0) {
                    entries.add(PieEntry(total.toFloat(), "", category))
                    colors.add(Color.parseColor(category.backgroundColor))
                    categoryIcons[category.id] = category.iconResId
                }
            }

            val dataSet = PieDataSet(entries, "").apply {
                setColors(colors)
                sliceSpace = 3f
                selectionShift = 10f
                valueTextColor = Color.BLACK
                valueTextSize = 13f
            }

            val pieData = PieData(dataSet)
            pieData.setValueFormatter(object : ValueFormatter() {
                private val formatter = DecimalFormat("#.##")
                override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                    val amount = formatter.format(value)
                    val percentage = formatter.format((value / totalSpending * 100))
                    return "R$amount\n($percentage%)"
                }
            })

            pieChart.data = pieData
            pieChart.setDrawEntryLabels(false)
            pieChart.setUsePercentValues(false)
            pieChart.setDrawCenterText(true)
            pieChart.setCenterText("Spending\nBreakdown")
            pieChart.setCenterTextSize(16f)
            pieChart.setHoleRadius(60f)
            pieChart.setTransparentCircleRadius(65f)
            pieChart.setEntryLabelTextSize(14f)
            pieChart.description = Description().apply { text = "" }
            pieChart.animateY(1000)
            pieChart.invalidate()

            pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val category = (e as? PieEntry)?.data as? CategoryEntity ?: return
                    val intent = Intent(this@MonthlyBreakdownActivity, AllExpensesActivity::class.java).apply {
                        putExtra("FILTER_CATEGORY_ID", category.id)
                        putExtra("FILTER_MONTH", selectedMonth)
                    }
                    startActivity(intent)
                }

                override fun onNothingSelected() {}
            })

            // Custom Legend: Icon + Label
            legendLayout.removeAllViews()
            categoryTotals.forEach { (categoryId, total) ->
                val category = allCategories.find { it.id == categoryId } ?: return@forEach
                val view = layoutInflater.inflate(R.layout.item_pie_legend, legendLayout, false)
                val iconView = view.findViewById<android.widget.ImageView>(R.id.legendIcon)
                val labelView = view.findViewById<TextView>(R.id.legendLabel)
                val iconDrawable = ContextCompat.getDrawable(this@MonthlyBreakdownActivity, category.iconResId)

                iconView.setImageDrawable(iconDrawable)
                labelView.text = category.name
                labelView.setTextColor(Color.parseColor(category.backgroundColor))

                legendLayout.addView(view)
            }

            goal?.let {
                tvGoalRange.text = "Goal: R${it.minGoal} - R${it.maxGoal}"
            } ?: run {
                tvGoalRange.text = "No goal set for this month"
            }
        }
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

    private fun getUsername(): String {
        val prefs = getSharedPreferences("WealthWhizPrefs", Context.MODE_PRIVATE)
        return prefs.getString("loggedInUsername", "") ?: ""
    }
}

