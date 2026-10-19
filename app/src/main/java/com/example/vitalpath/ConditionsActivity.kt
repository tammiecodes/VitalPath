package com.example.vitalpath

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.content.Intent

class ConditionsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conditions)

        val continueButton =
            findViewById<Button>(R.id.btnNextConditions)

        val backButton =
            findViewById<Button>(R.id.btnBackConditions)

        continueButton.setOnClickListener {
            val intent = Intent(this, SymptomsActivity::class.java)
            startActivity(intent)
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
