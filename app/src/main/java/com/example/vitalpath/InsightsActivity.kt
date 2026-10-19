package com.example.vitalpath

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Illustrative rules, not clinical diagnosis or personalized medical advice. */
class InsightsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insights)
        val store = LocalHealthStore(this)
        val logs = store.checkIns().filter { it.optString("date") >= LocalDate.now().minusDays(29).toString() }
        val events = store.doseEvents().filter { it.optLong("recordedMs") >=
                Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli() }
        val lines = mutableListOf<String>()
        lines += "Past 30 days • ${logs.size} logged day(s)"
        val bp = logs.mapNotNull { it.optString("bloodPressure").split("/").let { parts ->
            if (parts.size != 2) null else {
                val s = parts[0].toIntOrNull(); val d = parts[1].toIntOrNull()
                if (s == null || d == null) null else Pair(s,d)
            }
        } }
        if (bp.size >= 5) {
            val flagged = bp.count { it.first >= 130 || it.second >= 80 }
            val percent = (flagged * 100.0 / bp.size).toInt()
            lines += "\nBLOOD PRESSURE PATTERN"
            lines += "$flagged of ${bp.size} logged readings ($percent%) met the example 130/80 mmHg flag."
            if (percent >= 80) lines += "This happened in at least 80% of the recorded readings. Consider discussing your logs with a clinician."
        } else lines += "\nBlood pressure: At least 5 entries are needed for a percentage flag."

        val missed = events.filter { it.optString("status") == "missed" }
        lines += "\nMEDICATION ADHERENCE"
        lines += "${events.count { it.optString("status") == "taken" }} marked taken; ${missed.size} explicitly marked missed."
        lines += "Unmarked doses are not assumed to be missed."
        val missedDays = missed.map {
            Instant.ofEpochMilli(it.optLong("scheduledMs")).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        }.toSet()
        val paired = logs.filter { log ->
            log.optString("date") in missedDays && (log.optString("bloodSugar").toDoubleOrNull() ?: 0.0) >= 180.0
        }
        if (paired.isNotEmpty()) {
            lines += "\nGLUCOSE / MISSED-DOSE OVERLAP"
            lines += "${paired.size} day(s) had a marked missed dose and a glucose entry at or above the example 180 mg/dL flag."
            lines += "These events occurred on the same calendar day; this does NOT establish causation."
        }

        val dizzy = logs.filter { log ->
            val arr = log.optJSONArray("symptoms")
            arr != null && (0 until arr.length()).any { arr.optString(it).equals("Dizziness", true) }
        }.mapNotNull { runCatching { LocalDate.parse(it.optString("date")) }.getOrNull() }.toSet()
        var consecutive = 0; var longest = 0
        dizzy.sorted().forEachIndexed { i, d ->
            consecutive = if (i > 0 && dizzy.contains(d.minusDays(1))) consecutive + 1 else 1
            longest = maxOf(longest, consecutive)
        }
        lines += "\nDIZZINESS"
        lines += if (longest >= 3) "Dizziness was logged for $longest consecutive calendar days. Consider sharing this pattern with a clinician."
        else "Longest recorded consecutive-day streak: $longest. A flag appears at 3 days."
        lines += "\nRULE LIMITATIONS"
        lines += "130/80 mmHg and 180 mg/dL are demonstration thresholds, not patient-specific targets. Percentages use only days with measurements, not every day in the month."
        lines += "Missing data can distort patterns. This app does not diagnose conditions, determine treatment, or provide emergency monitoring."
        findViewById<TextView>(R.id.txtInsights).text = lines.joinToString("\n")
        findViewById<Button>(R.id.btnBackInsights).setOnClickListener { finish() }
    }
}
