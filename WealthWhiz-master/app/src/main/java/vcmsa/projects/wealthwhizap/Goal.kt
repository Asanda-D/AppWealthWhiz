package vcmsa.projects.wealthwhizap

data class Goal(
    val id: String = "",
    val userId: String, // NEW: ID of the user who created this goal
    val month: String,
    val minGoal: Double,
    val maxGoal: Double,
    val monthlyBudget: Double
)