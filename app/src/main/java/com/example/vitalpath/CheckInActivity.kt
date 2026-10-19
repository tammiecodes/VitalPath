package com.example.vitalpath

import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckInActivity : ComponentActivity() {
    private lateinit var store: LocalHealthStore
    private lateinit var date: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)
        store = LocalHealthStore(this)
        date = intent.getStringExtra("selectedDate")
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        findViewById<TextView>(R.id.txtCheckInDate).text = "Check-In for $date"

        val ids = listOf(R.id.cbCheckFatigue, R.id.cbCheckHeadache,
            R.id.cbCheckDizziness, R.id.cbCheckBreathing)
        val previous = store.checkIn(date)
        if (previous != null) {
            val savedSymptoms = previous.optJSONArray("symptoms") ?: JSONArray()
            val selected = (0 until savedSymptoms.length()).map { savedSymptoms.optString(it) }.toSet()
            ids.forEach { id ->
                findViewById<CheckBox>(id).apply { isChecked = text.toString() in selected }
            }
            findViewById<RatingBar>(R.id.ratingSeverity).rating = previous.optDouble("severity", 0.0).toFloat()
            fill(R.id.etHeartRate, previous.optString("heartRate"))
            fill(R.id.etBloodPressure, previous.optString("bloodPressure"))
            fill(R.id.etBloodSugar, previous.optString("bloodSugar"))
            fill(R.id.etSleepHours, previous.optString("sleepHours"))
            fill(R.id.etCheckInNotes, previous.optString("notes"))
        }

        findViewById<Button>(R.id.btnSaveCheckIn).setOnClickListener {
            val symptoms = JSONArray()
            ids.forEach { id ->
                findViewById<CheckBox>(id).apply { if (isChecked) symptoms.put(text.toString()) }
            }
            val entry = JSONObject()
                .put("date", date)
                .put("symptoms", symptoms)
                .put("severity", findViewById<RatingBar>(R.id.ratingSeverity).rating.toInt())
                .put("heartRate", value(R.id.etHeartRate))
                .put("bloodPressure", value(R.id.etBloodPressure))
                .put("bloodSugar", value(R.id.etBloodSugar))
                .put("sleepHours", value(R.id.etSleepHours))
                .put("notes", value(R.id.etCheckInNotes))
            store.putCheckIn(entry)
            Toast.makeText(this, "Check-in saved for $date", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.btnBackCheckIn).setOnClickListener { finish() }
    }

    private fun value(id: Int) = findViewById<EditText>(id).text.toString().trim()
    private fun fill(id: Int, text: String) = findViewById<EditText>(id).setText(text)
}
