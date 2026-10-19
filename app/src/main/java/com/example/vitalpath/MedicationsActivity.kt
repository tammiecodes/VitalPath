package com.example.vitalpath

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity

class MedicationsActivity : ComponentActivity() {
    private lateinit var store: LocalHealthStore
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medications)
        store = LocalHealthStore(this)
        list = findViewById(R.id.medicationList)
        findViewById<Button>(R.id.btnAddMedication).setOnClickListener { addDialog() }
        findViewById<Button>(R.id.btnMedicationReminders).setOnClickListener {
            Toast.makeText(this, "Reminders not implemented yet", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnBackMedications).setOnClickListener { finish() }
        refresh()
    }

    private fun addDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 16, 44, 8)
        }
        val name = EditText(this).apply { hint = "Medication name"; isSingleLine = true }
        val dose = EditText(this).apply { hint = "Dose (e.g., 10 mg)"; isSingleLine = true }
        val schedule = EditText(this).apply { hint = "Schedule (e.g., once daily)"; isSingleLine = true }
        container.addView(name); container.addView(dose); container.addView(schedule)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add medication (demo)")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (name.text.isBlank()) {
                    name.error = "Enter a medication name"
                } else {
                    store.addMedication(name.text.toString().trim(), dose.text.toString().trim(),
                        schedule.text.toString().trim())
                    dialog.dismiss()
                    refresh()
                }
            }
        }
        dialog.show()
    }

    private fun refresh() {
        list.removeAllViews()
        val meds = store.medications()
        if (meds.isEmpty()) {
            list.addView(TextView(this).apply { text = "No medications added yet."; textSize = 16f })
        }
        meds.forEach { med ->
            val label = listOf(med.optString("name"), med.optString("dose"), med.optString("schedule"))
                .filter { it.isNotBlank() }.joinToString(" · ")
            list.addView(TextView(this).apply {
                text = label; textSize = 16f; setPadding(0, 14, 0, 0)
            })
            list.addView(Button(this).apply {
                text = "Remove"
                setOnClickListener {
                    AlertDialog.Builder(this@MedicationsActivity)
                        .setMessage("Remove ${med.optString("name")}? ")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            store.removeMedication(med.optString("id")); refresh()
                        }.show()
                }
            })
        }
    }
}
