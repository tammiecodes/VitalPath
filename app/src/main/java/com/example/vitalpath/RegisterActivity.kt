package com.example.vitalpath

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity

class RegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        val continueButton = findViewById<Button>(R.id.btnContinue)
        val backButton = findViewById<Button>(R.id.btnBack)

        continueButton.setOnClickListener {
            Toast.makeText(
                this,
                "Health Setup coming next!",
                Toast.LENGTH_SHORT
            ).show()
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
