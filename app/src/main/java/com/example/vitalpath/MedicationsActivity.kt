package com.example.vitalpath

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.view.View

class MedicationsActivity : ComponentActivity() {
    private lateinit var store: LocalHealthStore
    private lateinit var list: LinearLayout
    private val navy = Color.rgb(16, 24, 39)
    private val white = Color.WHITE
    private val muted = Color.rgb(210, 225, 245)
    private val blue = Color.rgb(48, 120, 232)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medications)
        store = LocalHealthStore(this)
        list = findViewById(R.id.medicationList)
        findViewById<Button>(R.id.btnAddMedication).setOnClickListener { addDialog() }
        findViewById<Button>(R.id.btnMedicationReminders).setOnClickListener {
            Toast.makeText(this, "Notifications are not implemented yet", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnBackMedications).setOnClickListener { finish() }
        refresh()
    }

    private fun label(value: String): TextView = TextView(this).apply {
        text = value; textSize = 15f; setTextColor(white); setPadding(0, 10, 0, 6)
    }
    private fun button(value: String): Button = Button(this).apply {
        text = value; setTextColor(white); backgroundTintList =
        android.content.res.ColorStateList.valueOf(blue)
    }
    private fun dateText(epoch: Long): String = SimpleDateFormat(
        "MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(epoch))

    private fun askDateTime(done: (Long) -> Unit) {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            TimePickerDialog(this, { _, h, minute ->
                val date = Calendar.getInstance().apply {
                    set(y, m, d, h, minute, 0); set(Calendar.MILLISECOND, 0)
                }
                done(date.timeInMillis)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun addDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(36, 10, 36, 8)
        }
        val name = EditText(this).apply { hint = "Medication name"; isSingleLine = true }
        val dose = EditText(this).apply { hint = "Prescribed dose (e.g. 500 mg)"; isSingleLine = true }
        var count = 1
        var nextDue = 0L
        val times = button("1 time")
        val period = Spinner(this)
        period.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("day", "week", "month"))
        val dueButton = button("Choose first upcoming dose date/time")
        times.setOnClickListener {
            val picker = NumberPicker(this).apply { minValue = 1; maxValue = 4; value = count }
            val holder = LinearLayout(this).apply { gravity = Gravity.CENTER; addView(picker) }
            AlertDialog.Builder(this).setTitle("Times per period").setView(holder)
                .setNegativeButton("Cancel", null).setPositiveButton("Set") { _, _ ->
                    count = picker.value; times.text = "$count ${if (count == 1) "time" else "times"}"
                }.show()
        }
        dueButton.setOnClickListener { askDateTime { nextDue = it; dueButton.text = dateText(it) } }
        container.addView(name); container.addView(dose)
        container.addView(label("Frequency")); container.addView(times); container.addView(period)
        container.addView(label("First upcoming dose")); container.addView(dueButton)
        container.addView(label("Next doses are estimates based on evenly spaced intervals. Verify with your prescription."))
        val dialog = AlertDialog.Builder(this).setTitle("Add Medication")
            .setView(container).setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                when {
                    name.text.isBlank() -> name.error = "Name required"
                    dose.text.isBlank() -> dose.error = "Dose required"
                    nextDue <= 0 -> Toast.makeText(this, "Choose the first dose time", Toast.LENGTH_SHORT).show()
                    else -> {
                        val freq = "$count ${if (count == 1) "time" else "times"} per ${period.selectedItem}"
                        store.addMedication(name.text.toString().trim(), dose.text.toString().trim(), freq)
                        val id = store.medications().lastOrNull()?.optString("id")
                        if (id != null) store.setMedicationSchedule(id, nextDue, count, period.selectedItem.toString())
                        dialog.dismiss(); refresh()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun refresh() {
        list.removeAllViews()
        val meds = store.medications()
        if (meds.isEmpty()) list.addView(label("No medications added yet."))
        meds.forEach { med: JSONObject ->
            val id = med.optString("id")
            val next = med.optLong("nextDueMs", 0L)
            list.addView(label("${med.optString("name")} • ${med.optString("dose")}"))
            list.addView(label("Frequency: ${med.optString("schedule")}"))
            list.addView(label(if (next > 0L) "Estimated next dose: ${dateText(next)}"
            else "Next dose: Not set (older medication entry)"))
            val last = med.optString("lastStatus", "")
            if (last.isNotBlank()) list.addView(label("Last recorded dose: $last"))
            if (next <= 0L) {
                list.addView(button("Set upcoming dose").apply {
                    setOnClickListener { askDateTime { due ->
                        val schedule = med.optString("schedule")
                        val count = Regex("^([1-4])").find(schedule)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                        val period = when {
                            schedule.contains("week") -> "week"
                            schedule.contains("month") -> "month"
                            else -> "day"
                        }
                        store.setMedicationSchedule(id, due, count, period); refresh()
                    } }
                })
            } else {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                val taken = CheckBox(this).apply {
                    text = "Taken"; setTextColor(white); buttonTintList =
                    android.content.res.ColorStateList.valueOf(blue)
                }
                val missed = CheckBox(this).apply {
                    text = "Missed"; setTextColor(white); buttonTintList =
                    android.content.res.ColorStateList.valueOf(blue)
                }
                taken.setOnCheckedChangeListener { _, checked ->
                    if (checked) confirmDose(id, "taken")
                }
                missed.setOnCheckedChangeListener { _, checked ->
                    if (checked) confirmDose(id, "missed")
                }
                row.addView(taken); row.addView(missed); list.addView(row)
            }
            list.addView(button("Remove medication").apply {
                setOnClickListener {
                    AlertDialog.Builder(this@MedicationsActivity)
                        .setMessage("Remove ${med.optString("name")}? Recorded dose history is retained.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ -> store.removeMedication(id); refresh() }.show()
                }
            })
            list.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(-1, 2).apply { topMargin = 16; bottomMargin = 12 }
                setBackgroundColor(muted)
            })
        }
    }

    private fun confirmDose(id: String, status: String) {
        AlertDialog.Builder(this)
            .setTitle(if (status == "taken") "Confirm dose taken?" else "Mark scheduled dose as missed?")
            .setMessage("This records the status of the displayed scheduled dose and advances the estimated next time.")
            .setNegativeButton("Cancel") { _, _ -> refresh() }
            .setPositiveButton("Confirm") { _, _ ->
                store.recordDose(id, status); refresh()
            }.show()
    }
}
