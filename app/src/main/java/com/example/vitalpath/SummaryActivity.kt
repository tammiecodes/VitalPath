package com.example.vitalpath

import android.content.Intent
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import org.json.JSONObject
import java.time.LocalDate

class SummaryActivity : ComponentActivity() {
    private var start = LocalDate.now().minusDays(30)
    private var end = LocalDate.now()
    private lateinit var store: LocalHealthStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)
        store = LocalHealthStore(this)
        findViewById<Button>(R.id.btnStartDate).setOnClickListener {
            pick(start) { start = it; render() }
        }
        findViewById<Button>(R.id.btnEndDate).setOnClickListener {
            pick(end) { end = it; render() }
        }
        findViewById<Button>(R.id.btnOpenInsights).setOnClickListener {
            startActivity(Intent(this, InsightsActivity::class.java))
        }
        findViewById<Button>(R.id.btnBackSummary).setOnClickListener { finish() }
    }

    override fun onResume() { super.onResume(); render() }

    private fun pick(date: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(this, { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    private fun render() {
        findViewById<Button>(R.id.btnStartDate).text = "Start: $start"
        findViewById<Button>(R.id.btnEndDate).text = "End: $end"
        val symptomsText = findViewById<TextView>(R.id.txtSymptomHistory)
        val vitalsText = findViewById<TextView>(R.id.txtVitalHistory)
        if (start.isAfter(end)) {
            symptomsText.text = "Start date must be on or before end date."
            vitalsText.text = ""
            return
        }
        val entries = store.checkIns().filter {
            val day = it.optString("date")
            day >= start.toString() && day <= end.toString()
        }
        symptomsText.text = if (entries.isEmpty()) "No check-ins for this date range."
        else entries.joinToString("\n\n") { item: JSONObject ->
            val list = item.optJSONArray("symptoms")
            val symptoms = if (list == null || list.length() == 0) "None selected"
            else (0 until list.length()).joinToString(", ") { list.optString(it) }
            "${item.optString("date")}: $symptoms (severity ${item.optInt("severity")}/5)\n${item.optString("notes")}".trim()
        }
        vitalsText.text = if (entries.isEmpty()) "No vital measurements for this date range."
        else entries.joinToString("\n\n") { item ->
            "${item.optString("date")}: HR ${item.optString("heartRate", "—")} BPM; " +
                    "BP ${item.optString("bloodPressure", "—")}; " +
                    "Glucose ${item.optString("bloodSugar", "—")} mg/dL; " +
                    "Sleep ${item.optString("sleepHours", "—")} h"
        }
    }
}
