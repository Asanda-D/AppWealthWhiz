package vcmsa.projects.wealthwhizap

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vcmsa.projects.wealthwhizap.databinding.ActivityManageCategoriesBinding
import vcmsa.projects.wealthwhizap.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvUsername: TextView
    private lateinit var btnLogout: Button
    private val firebaseManager = FirebaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitleTextColor(Color.parseColor("#000D87"))
        supportActionBar?.title = "        \t\t\tUSER PROFILE"


        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvUsername = findViewById(R.id.tvUsername)
        btnLogout = findViewById(R.id.btnLogout)

        val userId = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
            .getString("userId", "")

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch user data from Firebase
        lifecycleScope.launch {
            firebaseManager.getUserData(userId).fold(
                onSuccess = { user ->
                    tvName.text = "Name: ${user.firstName}"
                    tvEmail.text = "Email: ${user.email}"
                    tvUsername.text = "Username: ${user.username}"
                },
                onFailure = { e ->
                    Toast.makeText(this@ProfileActivity, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        btnLogout.setOnClickListener {
            // Clear SharedPreferences
            val prefs = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
            with(prefs.edit()) {
                remove("loggedInUsername")
                remove("userId")
                apply()
            }

            // Sign out from Firebase
            firebaseManager.signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
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


