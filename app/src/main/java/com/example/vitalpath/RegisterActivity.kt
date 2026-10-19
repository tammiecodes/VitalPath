package com.example.vitalpath

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.ComponentActivity

class RegisterActivity : ComponentActivity() {
    private val notice = """
        VITAL PATH — PROTOTYPE PRIVACY & HEALTH DATA NOTICE

        Vital Path helps users record symptoms, vitals and medications. This development version
        saves manual tracking data in local, unencrypted app storage. It does not implement real
        authentication or secure cloud accounts. Do not enter real patient health information.

        This application is being designed to address applicable privacy and security requirements,
        including HIPAA where applicable. HIPAA compliance has NOT been established or certified.
        Acknowledgment confirms you saw this prototype notice, not a HIPAA disclosure authorization.

        Future integration: We plan to support Google Health Connect. No health data is accessed
        from Health Connect now. Later, permission to specific data types will be requested through
        Android's Health Connect permission interface and can be managed or revoked there.

        PDF exports may include sensitive information; protect them and choose recipients carefully.
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        findViewById<Button>(R.id.btnReadPrivacy).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Privacy & Health Data Notice")
                .setMessage(notice).setPositiveButton("Close", null).show()
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            val name = findViewById<EditText>(R.id.etName).text.toString().trim()
            val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
            val check = findViewById<CheckBox>(R.id.cbPrivacyAck)
            when {
                name.isEmpty() -> findViewById<EditText>(R.id.etName).error = "Enter a name"
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    findViewById<EditText>(R.id.etEmail).error = "Enter a valid email"
                !check.isChecked -> AlertDialog.Builder(this)
                    .setMessage("Please read and acknowledge the prototype privacy notice.")
                    .setPositiveButton("OK", null).show()
                else -> {
                    // Demo-only; no password stored or validated.
                    getSharedPreferences("vitalpath_profile", MODE_PRIVATE).edit()
                        .putString("name", name).putString("email", email)
                        .putLong("noticeAcknowledgedAt", System.currentTimeMillis())
                        .apply()
                    AlertDialog.Builder(this)
                        .setTitle("Future Health Connect Integration")
                        .setMessage("Vital Path may later import supported health data from Google Health Connect. " +
                                "No connection or health-data permission is requested now. " +
                                "You will be asked separately for permissions when this feature is available.")
                        .setPositiveButton("Continue") { _, _ ->
                            startActivity(Intent(this, ConditionsActivity::class.java))
                        }.show()
                }
            }
        }
    }
}
