package vcmsa.projects.wealthwhizap

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vcmsa.projects.wealthwhizap.databinding.ActivityAddExpenseBinding
import vcmsa.projects.wealthwhizap.databinding.ActivityManageCategoriesBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var etAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etSubCategory: EditText
    private lateinit var btnPickDateTime: Button
    private lateinit var tvDateTime: TextView
    private lateinit var etNotes: EditText
    private lateinit var btnPickImage: Button
    private lateinit var imagePreview: ImageView
    private lateinit var btnSaveExpense: Button


    private var selectedDateTime: String = ""
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private var isEditMode = false
    private var expenseId: String = ""
    private var selectedCategoryId: String = ""
    private val categories = mutableListOf<CategoryEntity>()

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private val firebaseManager = FirebaseManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitleTextColor(Color.parseColor("#000D87"))
        supportActionBar?.title = "    \t\t\tADD EXPENSE"


        etAmount = findViewById(R.id.etAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etSubCategory = findViewById(R.id.etSubCategory)
        btnPickDateTime = findViewById(R.id.btnPickDateTime)
        tvDateTime = findViewById(R.id.tvDateTime)
        etNotes = findViewById(R.id.etNotes)
        btnPickImage = findViewById(R.id.btnPickImage)
        imagePreview = findViewById(R.id.imagePreview)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)


        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        expenseId = intent.getStringExtra("EXPENSE_ID") ?: ""

        setupImagePicker()
        loadCategories()

        if (isEditMode) {
            loadExpenseData()
            btnSaveExpense.text = "Update Expense"
        }

        btnPickDateTime.setOnClickListener {
            showDateTimePicker()
        }

        btnPickImage.setOnClickListener {
            pickImageFromGallery()
        }

        btnSaveExpense.setOnClickListener {
            saveExpenseToDatabase()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val username = getCurrentUsername()
                firebaseManager.getCategories(username).fold(
                    onSuccess = { categoryList ->
                        categories.clear()
                        categories.addAll(categoryList)

                        if (categories.isEmpty()) {
                            // Create a default category if none exist
                            val defaultCategory = CategoryEntity(
                                id = "", // Temporary ID
                                userId = username,
                                name = "Uncategorized",
                                iconResId = R.drawable.ic_category_default,
                                backgroundColor = "#CCCCCC",
                                createdAt = System.currentTimeMillis()
                            )
                            lifecycleScope.launch {
                                firebaseManager.saveCategory(defaultCategory).fold(
                                    onSuccess = { categoryId ->
                                        // Create a new category with the Firebase ID
                                        val newCategory = defaultCategory.copy(id = categoryId)
                                        categories.add(newCategory)
                                        updateCategorySpinner()
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(this@AddExpenseActivity, "Error creating default category: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        } else {
                            updateCategorySpinner()
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(this@AddExpenseActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AddExpenseActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCategorySpinner() {
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(
            this@AddExpenseActivity,
            android.R.layout.simple_spinner_dropdown_item,
            categoryNames
        )
        spinnerCategory.adapter = adapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedCategoryId = categories[position].id
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategoryId = ""
            }
        }
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    // Request persistent URI permission
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(it, takeFlags)

                    selectedImageUri = it
                    loadAndScaleImage(it)
                } catch (e: SecurityException) {
                    // If we can't get persistent permission, try to get temporary permission
                    try {
                        contentResolver.openInputStream(it)?.use { stream ->
                            selectedImageUri = it
                            loadAndScaleImage(it)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error accessing image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadAndScaleImage(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get image dimensions
                val inputStream = contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                // Calculate scaling factor
                val maxDimension = 1024 // Maximum dimension for the image
                val scaleFactor = calculateScaleFactor(options.outWidth, options.outHeight, maxDimension)

                // Load and scale the image
                val scaledBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = scaleFactor
                    }
                    BitmapFactory.decodeStream(stream, null, options)
                }

                // Display the scaled image
                scaledBitmap?.let { bitmap ->
                    withContext(Dispatchers.Main) {
                        imagePreview.setImageBitmap(bitmap)
                        imagePreview.visibility = ImageView.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddExpenseActivity, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun calculateScaleFactor(width: Int, height: Int, maxDimension: Int): Int {
        var scaleFactor = 1
        if (width > maxDimension || height > maxDimension) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / scaleFactor) >= maxDimension && (halfHeight / scaleFactor) >= maxDimension) {
                scaleFactor *= 2
            }
        }
        return scaleFactor
    }

    private fun loadExpenseData() {
        lifecycleScope.launch {
            try {
                val username = getCurrentUsername()
                firebaseManager.getExpenses(username).fold(
                    onSuccess = { expenses ->
                        val expense = expenses.find { it.id == expenseId }
                        expense?.let {
                            runOnUiThread {
                                etAmount.setText(it.amount.toString())
                                val categoryPosition = categories.indexOfFirst { category -> category.id == it.categoryId }
                                if (categoryPosition != -1) {
                                    spinnerCategory.setSelection(categoryPosition)
                                }
                                etSubCategory.setText(it.subCategory ?: "")
                                tvDateTime.text = it.dateTime
                                selectedDateTime = it.dateTime
                                etNotes.setText(it.notes ?: "")

                                it.imageUri?.let { uri ->
                                    uploadedImageUrl = uri
                                    imagePreview.setImageURI(Uri.parse(uri))
                                    imagePreview.visibility = ImageView.VISIBLE
                                }
                            }
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(this@AddExpenseActivity, "Error loading expense: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AddExpenseActivity, "Error loading expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pickImageFromGallery() {
        imagePickerLauncher.launch("image/*")
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                selectedDateTime = dateFormat.format(calendar.time)
                tvDateTime.text = selectedDateTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveExpenseToDatabase() {
        val amountText = etAmount.text.toString()
        if (amountText.isEmpty()) {
            Toast.makeText(this, "Amount is required", Toast.LENGTH_SHORT).show()
            return
        }
        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategoryId.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val subCategory = etSubCategory.text.toString()

        if (selectedDateTime.isEmpty()) {
            Toast.makeText(this, "Please select a date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val notes = etNotes.text.toString()
        val username = getCurrentUsername()

        // Show loading indicator
        btnSaveExpense.isEnabled = false
        btnSaveExpense.text = "Saving..."

        lifecycleScope.launch {
            try {
                // First upload the image if there's a new one
                val imageUrl = if (selectedImageUri != null) {
                    Log.d("AddExpenseActivity", "Uploading new image...")
                    firebaseManager.uploadImage(selectedImageUri!!, username).fold(
                        onSuccess = { url ->
                            Log.d("AddExpenseActivity", "Image uploaded successfully: $url")
                            url
                        },
                        onFailure = { e ->
                            Log.e("AddExpenseActivity", "Error uploading image: ${e.message}", e)
                            Toast.makeText(applicationContext, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
                            null
                        }
                    )
                } else {
                    uploadedImageUrl
                }

                val expense = Expense(
                    id = if (isEditMode) expenseId else "",
                    username = username,
                    amount = amount,
                    categoryId = selectedCategoryId,
                    subCategory = subCategory,
                    dateTime = selectedDateTime,
                    notes = notes,
                    imageUri = imageUrl
                )

                if (isEditMode) {
                    firebaseManager.updateExpense(expense).fold(
                        onSuccess = {
                            Toast.makeText(applicationContext, "Expense updated!", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onFailure = { e ->
                            Toast.makeText(applicationContext, "Error updating expense: ${e.message}", Toast.LENGTH_SHORT).show()
                            btnSaveExpense.isEnabled = true
                            btnSaveExpense.text = "Update Expense"
                        }
                    )
                } else {
                    firebaseManager.saveExpense(expense).fold(
                        onSuccess = {
                            Toast.makeText(applicationContext, "Expense saved!", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onFailure = { e ->
                            Toast.makeText(applicationContext, "Error saving expense: ${e.message}", Toast.LENGTH_SHORT).show()
                            btnSaveExpense.isEnabled = true
                            btnSaveExpense.text = "Save Expense"
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("AddExpenseActivity", "Error saving expense: ${e.message}", e)
                Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSaveExpense.isEnabled = true
                btnSaveExpense.text = if (isEditMode) "Update Expense" else "Save Expense"
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

    private fun getCurrentUsername(): String {
        val sharedPrefs = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
        val username = sharedPrefs.getString("loggedInUsername", "")
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









