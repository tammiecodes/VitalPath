package com.example.vitalpath

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<Button>(R.id.btnEditProfile)
            .setOnClickListener {
                Toast.makeText(
                    this,
                    "Editing profile coming soon!",
                    Toast.LENGTH_SHORT
                ).show()
            }

        findViewById<Button>(R.id.btnEditConditions)
            .setOnClickListener {
                startActivity(
                    Intent(this, ConditionsActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnEditSymptoms)
            .setOnClickListener {
                startActivity(
                    Intent(this, SymptomsActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnEditPreferences)
            .setOnClickListener {
                startActivity(
                    Intent(this, PreferencesActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnBackProfile)
            .setOnClickListener {
                finish()
            }
    }
}
