package com.example.vitalpath

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarActivity : ComponentActivity() {

    private var selectedDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val calendar = findViewById<CalendarView>(
            R.id.healthCalendar
        )

        val dateText = findViewById<TextView>(
            R.id.txtSelectedDate
        )

        selectedDate = SimpleDateFormat(
            "yyyy-MM-dd", Locale.US
        ).format(Date())

        dateText.text = "Selected Date: $selectedDate"

        calendar.setOnDateChangeListener { _, year, month, day ->
            selectedDate = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                year, month + 1, day
            )
            dateText.text = "Selected Date: $selectedDate"
        }

        findViewById<Button>(R.id.btnOpenCheckIn)
            .setOnClickListener {
                val intent = Intent(
                    this,
                    CheckInActivity::class.java
                )
                intent.putExtra("selectedDate", selectedDate)
                startActivity(intent)
            }

        findViewById<Button>(R.id.btnBackCalendar)
            .setOnClickListener {
                finish()
            }
    }
}
