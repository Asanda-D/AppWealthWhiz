package vcmsa.projects.wealthwhizap

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions


class FirebaseManager(context: Context? = null) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage by lazy {
        try {
            FirebaseStorage.getInstance().apply {
                // Set maximum upload size to 10MB
                maxUploadRetryTimeMillis = 60000 // 1 minute
                maxOperationRetryTimeMillis = 60000 // 1 minute
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error initializing Firebase Storage: ${e.message}", e)
            throw e
        }
    }
    private val expensesCollection = db.collection("expenses")
    private val categoriesCollection = db.collection("categories")
    private val usersCollection = db.collection("users")
    private val goalsCollection = db.collection("goals")
    private val context = context

    init {
        try {
            // Verify storage is properly initialized
            Log.d("FirebaseManager", "Storage bucket: ${storage.reference.bucket}")
            Log.d("FirebaseManager", "Storage app: ${storage.app.name}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error in FirebaseManager initialization: ${e.message}", e)
        }
    }

    // Authentication methods
    suspend fun registerUser(
        email: String,
        password: String,
        firstName: String,
        username: String
    ): Result<String> {
        return try {
            // Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to create user")

            auth.currentUser?.getIdToken(true)?.await()

            // Create user document in Firestore
            val userData = hashMapOf(
                "firstName" to firstName,
                "email" to email,
                "username" to username,
                "createdAt" to System.currentTimeMillis()
            )


            usersCollection.document(userId).set(userData).await()
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




    suspend fun loginUser(username: String, password: String): Result<String> {
        return try {
            // First get the user's email from Firestore
            val userDoc = usersCollection
                .whereEqualTo("username", username)
                .get()
                .await()
                .documents
                .firstOrNull() ?: throw Exception("User not found")

            val email = userDoc.getString("email") ?: throw Exception("Invalid user data")

            // Now login with the email and password
            val authResult = auth.signInWithEmailAndPassword(email, password).await()

            auth.currentUser?.getIdToken(true)?.await()

            val userId = authResult.user?.uid ?: throw Exception("Failed to login")
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserData(userId: String): Result<User> {
        return try {
            val doc = usersCollection.document(userId).get().await()
            if (doc.exists()) {
                val user = User(
                    id = userId,
                    firstName = doc.getString("firstName") ?: "",
                    email = doc.getString("email") ?: "",
                    username = doc.getString("username") ?: "",
                    password = "" // Don't return password
                )
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    //kiki
    suspend fun getCurrentStreak(uid: String): Result<Int> {
        return try {
            val streak = db.collection("users").document(uid)
                .get().await()
                .getLong("loginStreak")?.toInt() ?: 0
            Result.success(streak)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // NEW: Update Login Streak On App Open (kiki)
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun updateLoginStreakOnAppOpen(uid: String): Result<Long> {
        return try {

            val userDocRef = db.collection("users").document(uid)
            val userSnapshot = userDocRef.get().await()

            val lastLoginStr = userSnapshot.getString("lastLoginDate")
            val loginStreak = userSnapshot.getLong("loginStreak")?.toInt() ?: 0

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val yesterdayStr = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.time)
            }

            val updates = mutableMapOf<String, Any>()

            if (lastLoginStr == null) {
                // First login
                updates["loginStreak"] = 1
                updates["lastLoginDate"] = todayStr
            } else if (lastLoginStr == yesterdayStr) {
                // Continue streak
                updates["loginStreak"] = loginStreak + 1
                updates["lastLoginDate"] = todayStr
            } else if (lastLoginStr != todayStr) {
                // Missed a day, reset streak
                updates["loginStreak"] = 1
                updates["lastLoginDate"] = todayStr
            } // else: already logged in today, do nothing

            if (updates.isNotEmpty()) {
                userDocRef.update(updates).await()
            }


            // 1) Read existing “lastLogin” & “currentStreak”
            val snap = userDocRef.get().await()
            val lastLoginTs = snap.getTimestamp("lastLogin")
            val oldStreak = snap.getLong("currentStreak") ?: 0L

            // 2) Compute “today” (midnight) & “yesterday” (midnight)
            val nowCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayDate = nowCal.time

            val yesterdayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val yesterdayDate = yesterdayCal.time

            // 3) Determine newStreak
            val newStreak = if (lastLoginTs == null) {
                1L
            } else {
                val lastLoginDate = lastLoginTs.toDate()
                when {
                    lastLoginDate == yesterdayDate -> oldStreak + 1
                    lastLoginDate.before(yesterdayDate) -> 1L
                    else -> oldStreak
                }
            }

            // 4) Write back to Firestore
            //val updates = mapOf(
            "lastLogin" to Timestamp.now()
            "currentStreak" to newStreak

            userDocRef.update(updates).await()

            Result.success(newStreak)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "updateLoginStreakOnAppOpen error", e)
            Result.failure(e)
        }

    }



    // Expense methods
    suspend fun saveExpense(expense: Expense): Result<String> {
        return try {
            val expenseData = hashMapOf(
                "username" to expense.username,
                "amount" to expense.amount,
                "categoryId" to expense.categoryId,
                "subCategory" to expense.subCategory,
                "dateTime" to expense.dateTime,
                "notes" to expense.notes,
                "imageUri" to expense.imageUri
            )
            val docRef = expensesCollection.add(expenseData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            val expenseData = hashMapOf(
                "username" to expense.username,
                "amount" to expense.amount,
                "categoryId" to expense.categoryId,
                "subCategory" to expense.subCategory,
                "dateTime" to expense.dateTime,
                "notes" to expense.notes,
                "imageUri" to expense.imageUri
            )
            expensesCollection.document(expense.id)
                .set(expenseData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExpenses(username: String): Result<List<Expense>> {
        return try {
            val snapshot = expensesCollection
                .whereEqualTo("username", username)
                .get()
                .await()

            val expenses = snapshot.documents.mapNotNull { doc ->
                try {
                    Expense(
                        id = doc.id,
                        username = doc.getString("username") ?: return@mapNotNull null,
                        amount = doc.getDouble("amount") ?: return@mapNotNull null,
                        categoryId = doc.getString("categoryId") ?: return@mapNotNull null,
                        subCategory = doc.getString("subCategory"),
                        dateTime = doc.getString("dateTime") ?: return@mapNotNull null,
                        notes = doc.getString("notes"),
                        imageUri = doc.getString("imageUri")
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error parsing expense: ${e.message}")
                    null
                }
            }.sortedByDescending { expense ->
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .parse(expense.dateTime)
                    date?.time ?: 0L
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error sorting expense: ${e.message}")
                    0L
                }
            }
            Result.success(expenses)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error getting expenses: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            expensesCollection.document(expenseId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExpensesByCategoryAndDateRange(
        username: String,
        categoryId: String,
        startDate: String,
        endDate: String
    ): Result<List<Expense>> {
        return try {
            val snapshot = expensesCollection
                .whereEqualTo("username", username)
                .get()
                .await()

            val expenses = snapshot.documents.mapNotNull { doc ->
                try {
                    Expense(
                        id = doc.id,
                        username = doc.getString("username") ?: return@mapNotNull null,
                        amount = doc.getDouble("amount") ?: return@mapNotNull null,
                        categoryId = doc.getString("categoryId") ?: return@mapNotNull null,
                        subCategory = doc.getString("subCategory"),
                        dateTime = doc.getString("dateTime") ?: return@mapNotNull null,
                        notes = doc.getString("notes"),
                        imageUri = doc.getString("imageUri")
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error parsing expense: ${e.message}")
                    null
                }
            }.filter { expense ->
                expense.categoryId == categoryId &&
                        expense.dateTime >= startDate &&
                        expense.dateTime <= endDate
            }.sortedByDescending { expense ->
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .parse(expense.dateTime)
                    date?.time ?: 0L
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error sorting expense: ${e.message}")
                    0L
                }
            }

            Log.d(
                "FirebaseManager",
                "Found ${expenses.size} expenses for category $categoryId between $startDate and $endDate"
            )
            Result.success(expenses)
        } catch (e: Exception) {
            Log.e(
                "FirebaseManager",
                "Error getting expenses by category and date range: ${e.message}"
            )
            Result.failure(e)
        }
    }

    // Category methods
    suspend fun saveCategory(category: CategoryEntity): Result<String> {
        return try {
            val categoryData = hashMapOf(
                "userId" to category.userId,
                "name" to category.name,
                "iconResId" to category.iconResId,
                "backgroundColor" to category.backgroundColor,
                "budget" to category.budget,
                "subcategory" to category.subcategory,
                "createdAt" to category.createdAt
            )
            val docRef = categoriesCollection.add(categoryData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategories(userId: String): Result<List<CategoryEntity>> {
        return try {
            val snapshot = categoriesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val categories = snapshot.documents.mapNotNull { doc ->
                try {
                    CategoryEntity(
                        id = doc.id,
                        userId = doc.getString("userId") ?: return@mapNotNull null,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        iconResId = doc.getLong("iconResId")?.toInt() ?: return@mapNotNull null,
                        backgroundColor = doc.getString("backgroundColor")
                            ?: return@mapNotNull null,
                        budget = doc.getDouble("budget"),
                        subcategory = doc.getString("subcategory"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategory(category: CategoryEntity): Result<Unit> {
        return try {
            val categoryData = hashMapOf(
                "userId" to category.userId,
                "name" to category.name,
                "iconResId" to category.iconResId,
                "backgroundColor" to category.backgroundColor,
                "budget" to category.budget,
                "subcategory" to category.subcategory
            )

            categoriesCollection.document(category.id)
                .set(categoryData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            categoriesCollection.document(categoryId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Goal methods
    suspend fun saveGoal(goal: Goal): Result<String> {
        return try {
            val goalData = hashMapOf(
                "username" to goal.userId,
                "month" to goal.month,
                "minGoal" to goal.minGoal,
                "maxGoal" to goal.maxGoal,
                "monthlyBudget" to goal.monthlyBudget,
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = goalsCollection.add(goalData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(goal: Goal): Result<Unit> {
        return try {
            val goalData = hashMapOf(
                "username" to goal.userId,
                "month" to goal.month,
                "minGoal" to goal.minGoal,
                "maxGoal" to goal.maxGoal,
                "monthlyBudget" to goal.monthlyBudget,
                "createdAt" to System.currentTimeMillis()
            )

            goalsCollection.document(goal.id)
                .set(goalData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goalId: String): Result<Unit> {
        return try {
            goalsCollection.document(goalId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoals(username: String): Result<List<Goal>> {
        return try {
            val snapshot = goalsCollection
                .whereEqualTo("username", username)
                .get()
                .await()

            val goals = snapshot.documents.mapNotNull { doc ->
                try {
                    Goal(
                        id = doc.id,
                        userId = doc.getString("username") ?: return@mapNotNull null,
                        month = doc.getString("month") ?: return@mapNotNull null,
                        minGoal = doc.getDouble("minGoal") ?: return@mapNotNull null,
                        maxGoal = doc.getDouble("maxGoal") ?: return@mapNotNull null,
                        monthlyBudget = doc.getDouble("monthlyBudget") ?: return@mapNotNull null
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { goal ->
                try {
                    val date = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(goal.month)
                    date?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoalByMonth(username: String, month: String): Result<Goal?> {
        return try {
            Log.d("FirebaseManager", "Getting goal for username: $username, month: $month")
            val snapshot = goalsCollection
                .whereEqualTo("username", username)
                .whereEqualTo("month", month)
                .get()
                .await()

            Log.d("FirebaseManager", "Found ${snapshot.documents.size} goals")

            val goal = snapshot.documents.firstOrNull()?.let { doc ->
                try {
                    Goal(
                        id = doc.id,
                        userId = doc.getString("username") ?: return@let null,
                        month = doc.getString("month") ?: return@let null,
                        minGoal = doc.getDouble("minGoal") ?: return@let null,
                        maxGoal = doc.getDouble("maxGoal") ?: return@let null,
                        monthlyBudget = doc.getDouble("monthlyBudget") ?: return@let null
                    ).also {
                        Log.d("FirebaseManager", "Parsed goal: $it")
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error parsing goal document: ${e.message}")
                    null
                }
            }
            Result.success(goal)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error getting goal by month: ${e.message}")
            Result.failure(e)
        }
    }

    // User methods
    suspend fun getUserByUsername(username: String): Result<User?> {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("username", username)
                .get()
                .await()

            val user = if (!snapshot.isEmpty) {
                val doc = snapshot.documents[0]
                try {
                    User(
                        id = doc.id,
                        firstName = doc.getString("firstName") ?: "",
                        email = doc.getString("email") ?: "",
                        username = doc.getString("username") ?: "",
                        password = "" // Don't return password
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Storage methods
    suspend fun uploadImage(imageUri: Uri, username: String): Result<String> {
        return try {
            Log.d("FirebaseManager", "Starting image save for user: $username")

            // Create a unique filename
            val timestamp = System.currentTimeMillis()
            val randomString = UUID.randomUUID().toString().substring(0, 8)
            val fileName = "${timestamp}_${randomString}.jpg"

            // Get the app's private storage directory
            val context =
                context ?: return Result.failure(Exception("Context is required for image save"))
            val imagesDir = File(context.filesDir, "expense_images/$username").apply { mkdirs() }
            val imageFile = File(imagesDir, fileName)

            // Copy the image to local storage
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Could not open image file"))

            // Return a file:// URI that can be used directly with Glide
            val fileUri = Uri.fromFile(imageFile).toString()
            Log.d("FirebaseManager", "Image saved successfully at: $fileUri")
            Result.success(fileUri)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving image: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imageUri: String): Result<Unit> {
        return try {
            // Convert URI string back to File
            val file = File(
                Uri.parse(imageUri).path ?: return Result.failure(Exception("Invalid image URI"))
            )
            if (file.exists()) {
                file.delete()
                Log.d("FirebaseManager", "Image deleted successfully: $imageUri")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error deleting image: ${e.message}")
            Result.failure(e)
        }
    }

    // Helper method to get image URI from local path
    fun getImageUriFromPath(imagePath: String): Uri? {
        return try {
            val imageFile = File(imagePath)
            if (imageFile.exists()) {
                Uri.fromFile(imageFile)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error getting image URI: ${e.message}")
            null
        }
    }


    companion object {
        private val db = FirebaseFirestore.getInstance()

        suspend fun getCurrentStreak(uid: String): Result<Int> {
            return try {
                val userDoc = db.collection("users").document(uid).get().await()
                val streak = userDoc.getLong("loginStreak")?.toInt() ?: 0
                Result.success(streak)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

