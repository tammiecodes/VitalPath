package com.example.vitalpath

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
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
    private var heartRate: String = ""
    private var systolic: String = ""
    private var diastolic: String = ""
    private var glucose: String = ""
    private var sleep: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)
        store = LocalHealthStore(this)
        date = intent.getStringExtra("selectedDate")
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        findViewById<TextView>(R.id.txtCheckInDate).text = "Check-In for $date"

        val ids = listOf(R.id.cbCheckFatigue, R.id.cbCheckHeadache,
            R.id.cbCheckDizziness, R.id.cbCheckBreathing)
        store.checkIn(date)?.let { previous ->
            val array = previous.optJSONArray("symptoms") ?: JSONArray()
            val chosen = (0 until array.length()).map { array.optString(it) }.toSet()
            ids.forEach { id ->
                findViewById<CheckBox>(id).apply { isChecked = text.toString() in chosen }
            }
            findViewById<RatingBar>(R.id.ratingSeverity).rating =
                previous.optDouble("severity", 0.0).toFloat()
            heartRate = previous.optString("heartRate")
            glucose = previous.optString("bloodSugar")
            sleep = previous.optString("sleepHours")
            val bp = previous.optString("bloodPressure").split("/")
            systolic = bp.getOrNull(0).orEmpty()
            diastolic = bp.getOrNull(1).orEmpty()
            findViewById<EditText>(R.id.etCheckInNotes).setText(previous.optString("notes"))
        }

        setupNumber(R.id.btnHeartRate, "Heart Rate", "BPM", 30, 220,
            { heartRate }, { heartRate = it })
        setupNumber(R.id.btnSystolic, "Systolic BP", "mmHg", 70, 250,
            { systolic }, { systolic = it })
        setupNumber(R.id.btnDiastolic, "Diastolic BP", "mmHg", 40, 150,
            { diastolic }, { diastolic = it })
        setupNumber(R.id.btnBloodSugar, "Blood Glucose", "mg/dL", 30, 500,
            { glucose }, { glucose = it })
        // Sleep spinner uses quarter-hour increments, such as 7.5 hours.
        setupNumber(R.id.btnSleepHours, "Sleep", "hours", 0, 96,
            { sleep }, { sleep = it }, 4)

        findViewById<Button>(R.id.btnSaveCheckIn).setOnClickListener {
            if (systolic.isNotBlank() != diastolic.isNotBlank()) {
                Toast.makeText(this, "Select both blood pressure values or neither", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val symptoms = JSONArray()
            ids.forEach { id ->
                findViewById<CheckBox>(id).apply { if (isChecked) symptoms.put(text.toString()) }
            }
            val bp = if (systolic.isNotBlank()) "$systolic/$diastolic" else ""
            val entry = JSONObject()
                .put("date", date)
                .put("symptoms", symptoms)
                .put("severity", findViewById<RatingBar>(R.id.ratingSeverity).rating.toInt())
                .put("heartRate", heartRate)
                .put("bloodPressure", bp)
                .put("bloodSugar", glucose)
                .put("sleepHours", sleep)
                .put("notes", findViewById<EditText>(R.id.etCheckInNotes).text.toString().trim())
            store.putCheckIn(entry)
            Toast.makeText(this, "Check-in saved for $date", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.btnBackCheckIn).setOnClickListener { finish() }
    }

    // Uses NumberPicker rather than an EditText. Blank values remain blank unless selected.
    private fun setupNumber(
        id: Int, label: String, unit: String, min: Int, max: Int,
        read: () -> String, write: (String) -> Unit, divisor: Int = 1
    ) {
        val button = findViewById<Button>(id)
        fun display() {
            val current = read()
            button.text = if (current.isBlank()) "$label: Not entered"
            else "$label: $current $unit"
        }
        display()
        button.setOnClickListener {
            val initial = (read().toDoubleOrNull()?.times(divisor)?.toInt()
                ?: (min + max) / 2).coerceIn(min, max)
            val picker = NumberPicker(this).apply {
                minValue = min
                maxValue = max
                value = initial
                wrapSelectorWheel = false
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                if (divisor != 1) {
                    displayedValues = (min..max).map {
                        String.format(Locale.US, "%.2f", it.toDouble() / divisor)
                    }.toTypedArray()
                }
            }
            val container = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                setPadding(20, 24, 20, 16)
                addView(picker)
            }
            AlertDialog.Builder(this)
                .setTitle("Select $label ($unit)")
                .setView(container)
                .setNeutralButton("Clear") { _, _ -> write(""); display() }
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Set") { _, _ ->
                    val chosen = picker.value.toDouble() / divisor
                    write(if (divisor == 1) picker.value.toString()
                    else String.format(Locale.US, "%.2f", chosen))
                    display()
                }
                .show()
        }
    }
}
