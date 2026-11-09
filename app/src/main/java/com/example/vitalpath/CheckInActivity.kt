package com.example.vitalpath

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckInActivity : ComponentActivity() {

    private lateinit var store: LocalHealthStore
    private lateinit var date: String

    private var heartRate = ""
    private var systolic = ""
    private var diastolic = ""
    private var glucose = ""
    private var sleep = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)

        store = LocalHealthStore(this)


        date = intent.getStringExtra("selectedDate")
            ?: SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).format(Date())

        findViewById<TextView>(R.id.txtCheckInDate).text =
            "Check-In for $date"

        val ids = listOf(
            R.id.cbCheckFatigue,
            R.id.cbCheckHeadache,
            R.id.cbCheckDizziness,
            R.id.cbCheckBreathing
        )

        // Load existing check-in from local storage.
        store.checkIn(date)?.let { previous ->

            val array = previous.optJSONArray("symptoms")
                ?: JSONArray()

            val chosen = (0 until array.length())
                .map { array.optString(it) }
                .toSet()

            ids.forEach { id ->
                findViewById<CheckBox>(id).apply {
                    isChecked = text.toString() in chosen
                }
            }

            findViewById<RatingBar>(R.id.ratingSeverity).rating =
                previous.optDouble("severity", 0.0).toFloat()

            heartRate = previous.optString("heartRate")
            glucose = previous.optString("bloodSugar")
            sleep = previous.optString("sleepHours")

            val bp = previous.optString("bloodPressure")
                .split("/")

            systolic = bp.getOrNull(0).orEmpty()
            diastolic = bp.getOrNull(1).orEmpty()

            findViewById<EditText>(R.id.etCheckInNotes)
                .setText(previous.optString("notes"))
        }

        // Number picker inputs.
        setupNumber(
            R.id.btnHeartRate, "Heart Rate", "BPM",
            30, 220,
            { heartRate }, { heartRate = it }
        )

        setupNumber(
            R.id.btnSystolic, "Systolic BP", "mmHg",
            70, 250,
            { systolic }, { systolic = it }
        )

        setupNumber(
            R.id.btnDiastolic, "Diastolic BP", "mmHg",
            40, 150,
            { diastolic }, { diastolic = it }
        )

        setupNumber(
            R.id.btnBloodSugar, "Blood Glucose", "mg/dL",
            30, 500,
            { glucose }, { glucose = it }
        )

        setupNumber(
            R.id.btnSleepHours, "Sleep", "hours",
            0, 96,
            { sleep }, { sleep = it }, 4
        )

        val saveButton = findViewById<Button>(R.id.btnSaveCheckIn)

        saveButton.setOnClickListener {

            if (systolic.isNotBlank() != diastolic.isNotBlank()) {
                Toast.makeText(
                    this,
                    "Select both blood pressure values or neither",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Require Firebase login before a cloud upload.
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                Toast.makeText(
                    this,
                    "Please log in before saving a check-in",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Collect selected symptoms.
            val symptoms = JSONArray()
            val symptomList = mutableListOf<String>()

            ids.forEach { id ->
                val checkbox = findViewById<CheckBox>(id)

                if (checkbox.isChecked) {
                    val symptom = checkbox.text.toString()
                    symptoms.put(symptom)
                    symptomList.add(symptom)
                }
            }

            val bp = if (systolic.isNotBlank()) {
                "$systolic/$diastolic"
            } else {
                ""
            }

            val severity = findViewById<RatingBar>(
                R.id.ratingSeverity
            ).rating.toInt()

            val notes = findViewById<EditText>(
                R.id.etCheckInNotes
            ).text.toString().trim()

            // Existing local JSON format.
            val entry = JSONObject()
                .put("date", date)
                .put("symptoms", symptoms)
                .put("severity", severity)
                .put("heartRate", heartRate)
                .put("bloodPressure", bp)
                .put("bloodSugar", glucose)
                .put("sleepHours", sleep)
                .put("notes", notes)

            // Preserve existing Calendar, Summary, PDF behavior.
            store.putCheckIn(entry)

            // Firestore-compatible data.
            val cloudEntry = hashMapOf(
                "date" to date,
                "symptoms" to symptomList,
                "severity" to severity,
                "heartRate" to heartRate,
                "bloodPressure" to bp,
                "bloodSugar" to glucose,
                "sleepHours" to sleep,
                "notes" to notes,
                "source" to "manual",
                "updatedAt" to
                        com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            saveButton.isEnabled = false

            // Save inside this authenticated user's records.
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("vital_logs")
                .document(date)
                .set(cloudEntry)
                .addOnSuccessListener {

                    saveButton.isEnabled = true

                    Toast.makeText(
                        this,
                        "Check-in saved locally and to Firebase!",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }
                .addOnFailureListener { error ->

                    saveButton.isEnabled = true

                    AlertDialog.Builder(this)
                        .setTitle("Cloud Save Failed")
                        .setMessage(
                            "Your check-in was saved locally, " +
                                    "but Firestore reported an error:\n\n" +
                                    error.localizedMessage
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }
        }

        findViewById<Button>(R.id.btnBackCheckIn)
            .setOnClickListener {
                finish()
            }
    }

    // Uses NumberPicker rather than an EditText.
    private fun setupNumber(
        id: Int,
        label: String,
        unit: String,
        min: Int,
        max: Int,
        read: () -> String,
        write: (String) -> Unit,
        divisor: Int = 1
    ) {
        val button = findViewById<Button>(id)

        fun display() {
            val current = read()

            button.text = if (current.isBlank()) {
                "$label: Not entered"
            } else {
                "$label: $current $unit"
            }
        }

        display()

        button.setOnClickListener {

            val initial = (
                    read().toDoubleOrNull()
                        ?.times(divisor)
                        ?.toInt()
                        ?: (min + max) / 2
                    ).coerceIn(min, max)

            val picker = NumberPicker(this).apply {
                minValue = min
                maxValue = max
                value = initial
                wrapSelectorWheel = false

                descendantFocusability =
                    NumberPicker.FOCUS_BLOCK_DESCENDANTS

                if (divisor != 1) {
                    displayedValues = (min..max).map {
                        String.format(
                            Locale.US,
                            "%.2f",
                            it.toDouble() / divisor
                        )
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
                .setNeutralButton("Clear") { _, _ ->
                    write("")
                    display()
                }
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Set") { _, _ ->

                    val chosen =
                        picker.value.toDouble() / divisor

                    write(
                        if (divisor == 1) {
                            picker.value.toString()
                        } else {
                            String.format(
                                Locale.US,
                                "%.2f",
                                chosen
                            )
                        }
                    )

                    display()
                }
                .show()
        }
    }
}
