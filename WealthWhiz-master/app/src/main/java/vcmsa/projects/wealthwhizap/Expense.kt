package vcmsa.projects.wealthwhizap

data class Expense(
    val id: String = "",
    val username: String, // NEW: Username to link expense to a user
    val amount: Double,
    val categoryId: String, // Reference to the category
    val subCategory: String?,
    val dateTime: String,
    val notes: String?,
    val imageUri: String?
)
