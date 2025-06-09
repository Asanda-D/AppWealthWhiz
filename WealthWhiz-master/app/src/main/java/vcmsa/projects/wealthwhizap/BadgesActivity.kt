package vcmsa.projects.wealthwhizap

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.wealthwhizap.databinding.ActivityBadgesBinding
import vcmsa.projects.wealthwhizap.databinding.ActivityManageCategoriesBinding
import java.util.Date

class BadgesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBadgesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBadgesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitleTextColor(Color.parseColor("#000D87"))
        supportActionBar?.title = "         \t\t\tBADGES"

        loadBadges()
    }

    private fun calculateBadgeIndex(signUpTimestamp: Timestamp): Int {
        val signUpDate = signUpTimestamp.toDate()
        val now = Date()
        val diffInMillis = now.time - signUpDate.time
        val daysElapsed = diffInMillis / (1000 * 60 * 60 * 24)
        return (daysElapsed / 35).toInt().coerceIn(0, 11) // Max 12 badges
    }

    private fun loadBadges() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val createdAtTimestamp = when (val createdAt = document.get("createdAt")) {
                    is Timestamp -> createdAt
                    is Long -> Timestamp(Date(createdAt))
                    is Double -> Timestamp(Date(createdAt.toLong()))
                    else -> null
                }

                if (createdAtTimestamp != null) {
                    val currentIndex = calculateBadgeIndex(createdAtTimestamp)

                    val badgeNames = listOf(
                        "Budgeting Noob", "Spare Change Collector", "Cautious Spender", "Savings Apprentice", "Smart Spender", "Money Tracker",
                        "Investment Seeker", "Cash Flow Analyst", "Expense Strategist", "Advisor", "Wealth Wizard", "Finance Master"
                    )

                    val badgeIcons = listOf(
                        R.drawable.ic_badge_1, R.drawable.ic_badge_2, R.drawable.ic_badge_3,
                        R.drawable.ic_badge_4, R.drawable.ic_badge_5, R.drawable.ic_badge_6,
                        R.drawable.ic_badge_7, R.drawable.ic_badge_8, R.drawable.ic_badge_9,
                        R.drawable.ic_badge_10, R.drawable.ic_badge_11, R.drawable.ic_badge_12
                    )

                    val badgeList = badgeNames.indices.map { i ->
                        BadgeItem(
                            name = badgeNames[i],
                            iconRes = badgeIcons[i],
                            unlocked = i <= currentIndex
                        )
                    }

                    val gridView: GridView = findViewById(R.id.badgesGridView)
                    gridView.adapter = BadgeAdapter(this, badgeList)
                } else {
                    Toast.makeText(this, "Sign-up date is missing or invalid.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load badges", Toast.LENGTH_SHORT).show()
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
}