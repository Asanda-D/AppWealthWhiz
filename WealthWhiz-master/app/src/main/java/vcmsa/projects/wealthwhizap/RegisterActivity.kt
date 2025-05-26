package vcmsa.projects.wealthwhizap

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var tvAlreadyHaveAccount: TextView
    private val firebaseManager = FirebaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etFirstName = findViewById(R.id.etFirstName)
        etEmail = findViewById(R.id.etEmail)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvAlreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount)

        btnSignUp.setOnClickListener {
            val firstName = etFirstName.text.toString()
            val email = etEmail.text.toString()
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (firstName.isBlank() || email.isBlank() || username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // Check if username already exists
                    firebaseManager.getUserByUsername(username).fold(
                        onSuccess = { existingUser ->
                            if (existingUser != null) {
                                Toast.makeText(this@RegisterActivity, "Username already exists", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            // Create user in Firebase Auth and Firestore
                            firebaseManager.registerUser(
                                email = email,
                                password = password,
                                firstName = firstName,
                                username = username
                            ).fold(
                                onSuccess = { userId ->
                                    Toast.makeText(this@RegisterActivity, "Registration Successful!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                                    finish()
                                },
                                onFailure = { e ->
                                    Toast.makeText(this@RegisterActivity, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        onFailure = { e ->
                            Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvAlreadyHaveAccount.setOnClickListener {
            finish()
        }
    }
}
