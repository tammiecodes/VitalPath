package com.example.vitalpath

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailField = findViewById<EditText>(R.id.etLoginEmail)
        val passwordField = findViewById<EditText>(R.id.etLoginPassword)
        val loginButton = findViewById<Button>(R.id.btnSubmitLogin)
        val backButton = findViewById<Button>(R.id.btnBackToWelcome)

        loginButton.setOnClickListener {

            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()

            when {
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    emailField.error = "Enter a valid email address"
                }

                password.isEmpty() -> {
                    passwordField.error = "Enter your password"
                }

                else -> {
                    // Prevent multiple login requests
                    loginButton.isEnabled = false

                    FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->

                            loginButton.isEnabled = true

                            if (task.isSuccessful) {

                                Toast.makeText(
                                    this,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(
                                    this,
                                    DashboardActivity::class.java
                                )

                                // Prevent returning to login using Back
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                                startActivity(intent)

                            } else {

                                Toast.makeText(
                                    this,
                                    "Login failed. Check your email and password.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
