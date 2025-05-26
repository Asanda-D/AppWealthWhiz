package vcmsa.projects.wealthwhizap

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

//@Database(
//    entities = [User::class, Expense::class, Goal::class, CategoryEntity::class], // Add all other entities here
//    version = 2,
//    exportSchema = false
//)
//abstract class AppDatabase : RoomDatabase() {
//
//    abstract fun userDao(): UserDao
//    abstract fun expenseDao(): ExpenseDao
//    abstract fun goalDao(): GoalDao
//    abstract fun categoryDao(): CategoryDao
//
//    companion object {
//        @Volatile private var INSTANCE: AppDatabase? = null
//
//        private val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                // First, create a temporary table for expenses
//                database.execSQL("""
//                    CREATE TABLE IF NOT EXISTS expenses_temp (
//                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
//                        username TEXT NOT NULL,
//                        amount REAL NOT NULL,
//                        categoryId INTEGER NOT NULL,
//                        subCategory TEXT,
//                        dateTime TEXT NOT NULL,
//                        notes TEXT,
//                        imageUri TEXT,
//                        FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE CASCADE
//                    )
//                """)
//
//                // Create categories table
//                database.execSQL("""
//                    CREATE TABLE IF NOT EXISTS categories (
//                        id TEXT PRIMARY KEY NOT NULL,
//                        userId TEXT NOT NULL,
//                        name TEXT NOT NULL,
//                        iconResId INTEGER NOT NULL,
//                        backgroundColor TEXT NOT NULL,
//                        budget REAL,
//                        subcategory TEXT,
//                        createdAt INTEGER NOT NULL
//                    )
//                """)
//
//                // Copy data from old expenses table to new one
//                database.execSQL("""
//                    INSERT INTO expenses_temp (id, username, amount, categoryId, subCategory, dateTime, notes, imageUri)
//                    SELECT id, username, amount, categoryId, subCategory, dateTime, notes, imageUri FROM expenses
//                """)
//
//                // Drop old expenses table
//                database.execSQL("DROP TABLE expenses")
//
//                // Rename new expenses table
//                database.execSQL("ALTER TABLE expenses_temp RENAME TO expenses")
//            }
//        }
//
//        fun getInstance(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "wealthwhiz_database"
//                )
//                .addMigrations(MIGRATION_1_2)
//                .build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
//}
