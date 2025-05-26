package vcmsa.projects.wealthwhizap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.tasks.await

/**
 * Repository class for handling category data operations.
 * Acts as a mediator between the ViewModel and Firestore.
 */
class CategoryRepository {
    private val firebaseManager = FirebaseManager()

    suspend fun getAllCategories(userId: String): Result<List<CategoryEntity>> {
        return firebaseManager.getCategories(userId)
    }

    suspend fun searchCategories(userId: String, searchQuery: String): Result<List<CategoryEntity>> {
        return firebaseManager.getCategories(userId).map { categories ->
            categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    suspend fun getCategoryById(userId: String, categoryId: String): Result<CategoryEntity?> {
        return firebaseManager.getCategories(userId).map { categories ->
            categories.find { it.id == categoryId }
        }
    }

    suspend fun insert(category: CategoryEntity): Result<String> {
        return firebaseManager.saveCategory(category)
    }

    suspend fun update(category: CategoryEntity): Result<Unit> {
        return firebaseManager.updateCategory(category)
    }

    suspend fun delete(category: CategoryEntity): Result<Unit> {
        return firebaseManager.deleteCategory(category.id)
    }

    suspend fun deleteAllCategoriesForUser(userId: String): Result<Unit> {
        return try {
            val categories = firebaseManager.getCategories(userId).getOrNull() ?: return Result.failure(Exception("Failed to get categories"))
            for (category in categories) {
                firebaseManager.deleteCategory(category.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}