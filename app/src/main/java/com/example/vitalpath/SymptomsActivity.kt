package com.example.vitalpath

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.content.Intent

class SymptomsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptoms)

        val continueButton =
            findViewById<Button>(R.id.btnNextSymptoms)

        val backButton =
            findViewById<Button>(R.id.btnBackSymptoms)


        continueButton.setOnClickListener {
            val intent = Intent(this, PreferencesActivity::class.java)
            startActivity(intent)
        }


        backButton.setOnClickListener {
            finish()
        }
    }
}
