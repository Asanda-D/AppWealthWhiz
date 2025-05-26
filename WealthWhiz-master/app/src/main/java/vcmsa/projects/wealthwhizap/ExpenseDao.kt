package vcmsa.projects.wealthwhizap

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE username = :username ORDER BY dateTime DESC")
    fun getAllExpenses(username: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE username = :username AND categoryId = :categoryId ORDER BY dateTime DESC")
    fun getExpensesByCategory(username: String, categoryId: Int): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE username = :username AND categoryId = :categoryId")
    fun getTotalExpensesForCategory(username: String, categoryId: Int): Flow<Double>

    @Query("SELECT * FROM expenses WHERE username = :username AND categoryId = :categoryId AND dateTime BETWEEN :startDate AND :endDate ORDER BY dateTime DESC")
    fun getExpensesByCategoryAndDateRange(username: String, categoryId: Int, startDate: String, endDate: String): Flow<List<Expense>>

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE username = :username")
    suspend fun deleteAllExpenses(username: String)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Int): Expense?
}