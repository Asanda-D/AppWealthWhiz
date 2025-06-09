package vcmsa.projects.wealthwhizap

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import vcmsa.projects.wealthwhizap.databinding.ActivityEditCategoryBinding

class EditCategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditCategoryBinding
    private lateinit var viewModel: CategoryViewModel
    private lateinit var category: CategoryEntity
    private var selectedIconResId: Int = R.drawable.ic_food
    private var selectedColorHex: String = "#FFC107"
    private lateinit var alertDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitleTextColor(Color.parseColor("#000D87"))
        supportActionBar?.title = "       \t\t\tEDIT CATEGORY"

        // Get category from intent
        category = intent.getParcelableExtra("category")
            ?: throw IllegalStateException("Category not provided")

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

        // Pre-fill fields
        binding.etCategoryName.setText(category.name)
        binding.etSubcategoryName.setText(category.subcategory ?: "")
        binding.etCategoryBudget.setText(category.budget?.toString() ?: "")
        binding.ivSelectedIcon.setImageResource(category.iconResId)
        binding.colorPreview.setBackgroundColor(android.graphics.Color.parseColor(category.backgroundColor))

        selectedIconResId = category.iconResId
        selectedColorHex = category.backgroundColor

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnChooseIcon.setOnClickListener {
            showIconPickerDialog()
        }

        binding.btnChooseColor.setOnClickListener {
            showColorPickerDialog()
        }

        binding.btnSave.setOnClickListener {
            saveCategory()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.errorMessage.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveCategory() {
        val name = binding.etCategoryName.text.toString().trim()
        val sub = binding.etSubcategoryName.text.toString().trim()
        val budget = binding.etCategoryBudget.text.toString().trim().toDoubleOrNull()

        if (name.isEmpty()) {
            Toast.makeText(this, "Category name is required", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedCategory = category.copy(
            name = name,
            subcategory = if (sub.isEmpty()) null else sub,
            budget = budget,
            iconResId = selectedIconResId,
            backgroundColor = selectedColorHex
        )

        viewModel.update(updatedCategory)
        Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete this category?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.delete(category)
                Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showIconPickerDialog() {
        val icons = listOf(
            R.drawable.ic_food,
            R.drawable.ic_transport,
            R.drawable.ic_groceries,
            R.drawable.ic_utilities,
            R.drawable.ic_rent,
            R.drawable.ic_fuel,
            R.drawable.ic_work,
            R.drawable.ic_savings,
            R.drawable.ic_travel,
            R.drawable.ic_outings,
            R.drawable.ic_heart,
            R.drawable.ic_hobbies,
            R.drawable.ic_gift,
            R.drawable.ic_world,
            R.drawable.ic_home,
            R.drawable.ic_entertainment
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_icon_picker, null)
        val gridLayout = dialogView.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.gridIcons)

        for (icon in icons) {
            val iconView = android.widget.ImageView(this)
            iconView.setImageResource(icon)
            iconView.layoutParams = android.widget.LinearLayout.LayoutParams(120, 120)
            iconView.setPadding(16, 16, 16, 16)

            iconView.setOnClickListener {
                selectedIconResId = icon
                binding.ivSelectedIcon.setImageResource(icon)
                alertDialog.dismiss()
            }

            gridLayout.addView(iconView)
        }

        alertDialog = AlertDialog.Builder(this)
            .setTitle("Choose Icon")
            .setView(dialogView)
            .create()

        alertDialog.show()
    }

    private fun showColorPickerDialog() {
        val colors = listOf(
            "#FFC107", "#03A9F4", "#8BC34A", "#FF5722", "#9C27B0", "#996500", "#D80000", "#C6D9C8",
            "#4CAF50", "#E91E63", "#009688", "#FF9800", "#607D8B", "#F5B6CB", "#5C311F", "#BFCAD9"
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val gridLayout = dialogView.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.gridColors)

        for (color in colors) {
            val colorView = android.view.View(this)
            val size = resources.getDimensionPixelSize(R.dimen.color_circle_size)
            val margin = resources.getDimensionPixelSize(R.dimen.color_circle_margin)

            val params = android.view.ViewGroup.MarginLayoutParams(size, size)
            params.setMargins(margin, margin, margin, margin)

            colorView.layoutParams = params
            colorView.setBackgroundColor(android.graphics.Color.parseColor(color))

            colorView.setOnClickListener {
                selectedColorHex = color
                binding.colorPreview.setBackgroundColor(android.graphics.Color.parseColor(color))
                alertDialog.dismiss()
            }

            gridLayout.addView(colorView)
        }

        alertDialog = AlertDialog.Builder(this)
            .setTitle("Choose Color")
            .setView(dialogView)
            .create()

        alertDialog.show()
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

    companion object {
        fun newIntent(context: android.content.Context, category: CategoryEntity): Intent {
            return Intent(context, EditCategoryActivity::class.java).apply {
                putExtra("category", category)
            }
        }
    }
}