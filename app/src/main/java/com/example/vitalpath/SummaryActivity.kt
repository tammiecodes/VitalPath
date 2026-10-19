package com.example.vitalpath

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import java.util.Calendar
import java.util.Locale

class SummaryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val startButton =
            findViewById<Button>(R.id.btnStartDate)
        val endButton =
            findViewById<Button>(R.id.btnEndDate)

        startButton.setOnClickListener {
            chooseDate(startButton, "Start")
        }

        endButton.setOnClickListener {
            chooseDate(endButton, "End")
        }

        findViewById<Button>(R.id.btnBackSummary)
            .setOnClickListener {
                finish()
            }
    }

    private fun chooseDate(button: Button, label: String) {
        val today = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val date = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    year, month + 1, day
                )
                button.text = "$label: $date"
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
