package com.example.vitalpath

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.content.Intent

class RegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        val continueButton = findViewById<Button>(R.id.btnContinue)
        val backButton = findViewById<Button>(R.id.btnBack)


        continueButton.setOnClickListener {
            val intent = Intent(this, ConditionsActivity::class.java)
            startActivity(intent)
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
