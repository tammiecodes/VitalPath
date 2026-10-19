package com.example.vitalpath

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity

class MedicationsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medications)

        findViewById<Button>(R.id.btnAddMedication)
            .setOnClickListener {
                Toast.makeText(
                    this,
                    "Add Medication coming soon!",
                    Toast.LENGTH_SHORT
                ).show()
            }

        findViewById<Button>(R.id.btnMedicationReminders)
            .setOnClickListener {
                Toast.makeText(
                    this,
                    "Medication reminders coming soon!",
                    Toast.LENGTH_SHORT
                ).show()
            }

        findViewById<Button>(R.id.btnBackMedications)
            .setOnClickListener {
                finish()
            }
    }
}
