package com.example.vitalpath

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONObject
import java.time.LocalDate

class ReportActivity : ComponentActivity() {
    private var start = LocalDate.now().minusDays(30)
    private var end = LocalDate.now()
    private lateinit var store: LocalHealthStore

    private val savePdf = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            val pdf = createPdf()
            try {
                contentResolver.openOutputStream(uri)?.use { stream -> pdf.writeTo(stream) }
                    ?: error("Unable to open output file")
                Toast.makeText(this, "Report saved", Toast.LENGTH_LONG).show()
            } finally { pdf.close() }
        } catch (error: Exception) {
            Toast.makeText(this, "PDF error: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        store = LocalHealthStore(this)
        findViewById<Button>(R.id.btnReportStartDate).setOnClickListener {
            pick(start) { start = it; render() }
        }
        findViewById<Button>(R.id.btnReportEndDate).setOnClickListener {
            pick(end) { end = it; render() }
        }
        findViewById<Button>(R.id.btnGeneratePDF).setOnClickListener {
            if (start.isAfter(end)) Toast.makeText(this, "Check your date range", Toast.LENGTH_SHORT).show()
            else savePdf.launch("VitalPath_Report_${LocalDate.now()}.pdf")
        }
        findViewById<Button>(R.id.btnBackReport).setOnClickListener { finish() }
    }

    override fun onResume() { super.onResume(); render() }

    private fun pick(date: LocalDate, done: (LocalDate) -> Unit) {
        DatePickerDialog(this, { _, y, m, d -> done(LocalDate.of(y, m + 1, d)) },
            date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    private fun entries() = store.checkIns().filter {
        val d = it.optString("date"); d >= start.toString() && d <= end.toString()
    }

    private fun render() {
        findViewById<Button>(R.id.btnReportStartDate).text = "Start: $start"
        findViewById<Button>(R.id.btnReportEndDate).text = "End: $end"
        val count = if (start.isAfter(end)) "Invalid date range" else "${entries().size} check-in(s)"
        findViewById<TextView>(R.id.txtReportPreview).text =
            "Report period: $start to $end\n$count\n" +
                    "Current medications: ${store.medications().size}\n\n" +
                    "The PDF includes a confidentiality cover page and patient-reported data."
    }

    private fun createPdf(): PdfDocument {
        val pdf = PdfDocument()
        val width = 595
        val height = 842
        val margin = 48f
        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 11f }
        val heading = Paint(regular).apply { textSize = 20f; typeface = Typeface.DEFAULT_BOLD }
        val strong = Paint(regular).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 12f }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = margin

        fun newPage() {
            page?.let { pdf.finishPage(it) }
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            y = margin
        }

        fun line(value: String, paint: Paint = regular, extra: Float = 6f) {
            val maxWidth = width - margin * 2
            val words = value.split(Regex("\\s+"))
            var current = ""
            fun draw(text: String) {
                if (y + paint.textSize + extra > height - margin) newPage()
                page!!.canvas.drawText(text, margin, y, paint)
                y += paint.textSize + extra
            }
            for (word in words) {
                val proposal = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(proposal) > maxWidth && current.isNotEmpty()) {
                    draw(current)
                    current = word
                } else current = proposal
            }
            if (current.isNotEmpty()) draw(current) else y += paint.textSize + extra
        }

        // Cover page — careful not to claim legal compliance or restrict patient access.
        newPage()
        y = 160f
        line("VITAL PATH", heading, 22f)
        line("CONFIDENTIAL PERSONAL HEALTH REPORT", strong, 25f)
        line("Intended for the patient and healthcare recipients the patient authorizes.")
        line("Patient-controlled export: handle and share carefully.")
        y += 35f
        line("Patient: " + getSharedPreferences("vitalpath_profile", MODE_PRIVATE)
            .getString("name", "Not provided"))
        line("Period: $start through $end")
        line("Generated: ${LocalDate.now()}")
        y += 50f
        line("HIPAA / PRIVACY INFORMATION", strong)
        line("HIPAA may apply to certain recipients or services, depending on their legal role. " +
                "This exported file is not itself a certification of HIPAA compliance.")
        line("Vital Path is a prototype personal tracking app, not a medical record verified by a clinician.")
        line("Do not distribute without the patient's permission. Do not use this report as a diagnosis.")

        newPage()
        line("Patient-reported health summary", heading, 16f)
        line("Dates: $start to $end")
        val meds = store.medications()
        y += 8f
        line("Current medication list (${meds.size})", strong)
        line("Medication information is entered by the user; not verified against pharmacy records.")
        if (meds.isEmpty()) line("No medications entered.")
        meds.forEach { med ->
            line("• ${med.optString("name")} | Dose: ${med.optString("dose", "Not specified")} | " +
                    "Schedule: ${med.optString("schedule", "Not specified")}")
        }
        y += 8f
        line("Medication dose confirmations (selected report dates)", strong)
        val doseEvents = store.doseEvents().filter { event ->
            val localDate = java.time.Instant.ofEpochMilli(event.optLong("scheduledMs"))
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            !localDate.isBefore(start) && !localDate.isAfter(end)
        }
        if (doseEvents.isEmpty()) line("No doses explicitly marked taken or missed during this period.")
        doseEvents.forEach { event ->
            val whenDue = java.time.Instant.ofEpochMilli(event.optLong("scheduledMs"))
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            line("$whenDue | ${event.optString("medName")} | ${event.optString("status")}")
        }
        y += 12f
        val logs = entries()
        line("Daily check-ins (${logs.size})", strong)
        if (logs.isEmpty()) line("No check-ins for the chosen dates.")
        logs.forEach { item: JSONObject ->
            y += 8f
            line(item.optString("date"), strong)
            val array = item.optJSONArray("symptoms")
            val symptoms = if (array == null || array.length() == 0) "None selected" else
                (0 until array.length()).joinToString(", ") { array.optString(it) }
            line("Symptoms: $symptoms; severity ${item.optInt("severity")}/5")
            line("Heart rate: ${item.optString("heartRate", "—")} BPM; " +
                    "Blood pressure: ${item.optString("bloodPressure", "—")}")
            line("Blood sugar: ${item.optString("bloodSugar", "—")} mg/dL; " +
                    "Sleep: ${item.optString("sleepHours", "—")} hours")
            line("Notes: ${item.optString("notes").ifBlank { "None" }}")
        }
        page?.let { pdf.finishPage(it) }
        return pdf
    }
}
