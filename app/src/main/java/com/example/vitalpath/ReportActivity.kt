package com.example.vitalpath

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.util.Calendar
import java.util.Locale

class ReportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val startButton =
            findViewById<Button>(R.id.btnReportStartDate)
        val endButton =
            findViewById<Button>(R.id.btnReportEndDate)

        startButton.setOnClickListener {
            chooseDate(startButton, "Start")
        }

        endButton.setOnClickListener {
            chooseDate(endButton, "End")
        }

        findViewById<Button>(R.id.btnGeneratePDF)
            .setOnClickListener {
                Toast.makeText(
                    this,
                    "PDF generation coming soon!",
                    Toast.LENGTH_SHORT
                ).show()
            }

        findViewById<Button>(R.id.btnBackReport)
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