package vcmsa.projects.wealthwhizap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CategoryRepository()
    private val _currentUserId = MutableLiveData<String?>()
    private val _errorMessage = MutableLiveData<String>()
    private val _categories = MutableLiveData<List<CategoryEntity>>()

    val errorMessage: LiveData<String> = _errorMessage
    val categories: LiveData<List<CategoryEntity>> = _categories

    fun setCurrentUserId(userId: String) {
        _currentUserId.value = userId
        loadCategories()
    }

    fun loadCategories() {
        val userId = _currentUserId.value
        if (userId == null) {
            _errorMessage.value = "User ID not set"
            return
        }

        viewModelScope.launch {
            try {
                repository.getAllCategories(userId).fold(
                    onSuccess = { categories ->
                        _categories.value = categories
                    },
                    onFailure = { e ->
                        _errorMessage.value = "Failed to load categories: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load categories: ${e.message}"
            }
        }
    }

    fun searchCategories(query: String) {
        val userId = _currentUserId.value
        if (userId == null) {
            _errorMessage.value = "User ID not set"
            return
        }

        viewModelScope.launch {
            try {
                repository.searchCategories(userId, query).fold(
                    onSuccess = { categories ->
                        _categories.value = categories
                    },
                    onFailure = { e ->
                        _errorMessage.value = "Failed to search categories: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to search categories: ${e.message}"
            }
        }
    }

    fun insert(category: CategoryEntity) = viewModelScope.launch {
        try {
            repository.insert(category).fold(
                onSuccess = {
                    loadCategories() // Reload categories after successful insert
                },
                onFailure = { e ->
                    _errorMessage.value = "Failed to insert category: ${e.message}"
                }
            )
        } catch (e: Exception) {
            _errorMessage.value = "Failed to insert category: ${e.message}"
        }
    }

    fun update(category: CategoryEntity) = viewModelScope.launch {
        try {
            repository.update(category).fold(
                onSuccess = {
                    loadCategories() // Reload categories after successful update
                },
                onFailure = { e ->
                    _errorMessage.value = "Failed to update category: ${e.message}"
                }
            )
        } catch (e: Exception) {
            _errorMessage.value = "Failed to update category: ${e.message}"
        }
    }

    fun delete(category: CategoryEntity) = viewModelScope.launch {
        try {
            repository.delete(category).fold(
                onSuccess = {
                    loadCategories() // Reload categories after successful delete
                },
                onFailure = { e ->
                    _errorMessage.value = "Failed to delete category: ${e.message}"
                }
            )
        } catch (e: Exception) {
            _errorMessage.value = "Failed to delete category: ${e.message}"
        }
    }
}