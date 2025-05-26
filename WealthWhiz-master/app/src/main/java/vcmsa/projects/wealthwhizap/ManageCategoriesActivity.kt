package vcmsa.projects.wealthwhizap

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import vcmsa.projects.wealthwhizap.databinding.ActivityManageCategoriesBinding

class ManageCategoriesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageCategoriesBinding
    private lateinit var viewModel: CategoryViewModel
    private lateinit var categoriesAdapter: CategoriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Categories"

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[CategoryViewModel::class.java]

        // Get current user ID from SharedPreferences
        val sharedPref = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getString("loggedInUsername", "")

        if (currentUserId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.setCurrentUserId(currentUserId)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup FAB for adding new category
        binding.fabAddCategory.setOnClickListener {
            startActivity(AddCategoryActivity.newIntent(this))
        }

        // Observe categories
        viewModel.categories.observe(this) { categories ->
            categoriesAdapter.updateData(categories)
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter(emptyList()) { category ->
            startActivity(EditCategoryActivity.newIntent(this, category))
        }

        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(this@ManageCategoriesActivity, 2)
            adapter = categoriesAdapter
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

    override fun onResume() {
        super.onResume()
        // Refresh categories when returning to this activity
        viewModel.loadCategories()
        Log.d("ManageCategoriesActivity", "Categories loaded: ${viewModel.categories.value?.size ?: 0}")
    }
}