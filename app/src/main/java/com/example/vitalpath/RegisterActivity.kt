package com.example.vitalpath

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.ComponentActivity

class RegisterActivity : ComponentActivity() {
    // DRAFT FOR TEAM REVIEW: Adapt legal contact, retention and deployment disclosures before public use.
    private val notice = """
        VITAL PATH — PRIVACY, HEALTH DATA & TERMS NOTICE
        Version: Prototype draft 1.0 | October 2026

        1. PURPOSE
        Vital Path helps individuals manually track symptoms, vital signs, medications,
        and daily health observations. It provides summaries that individuals may choose
        to bring to healthcare appointments. It does not diagnose, prescribe, monitor
        emergencies, or replace professional medical care.

        2. INFORMATION ENTERED
        The prototype may store a profile name and email; health conditions and symptoms
        selected by the user; daily logs, blood pressure, heart rate, blood glucose,
        sleep, medications, and notes; and the date the privacy notice was acknowledged.
        Some planned features are not yet connected or implemented.

        3. WHERE YOUR DATA GOES — CURRENT PROTOTYPE
        Manual health entries and medications are stored in an unencrypted JSON file
        within this device's app-private storage. Demo profile information is held in
        local application preferences. There is no implemented backend account login,
        cloud sync, clinician portal, or automatic transmission to physicians.
        Do not use real patient information in this development build.

        4. SHARING & EXPORTS
        The user may choose to export a PDF report using Android's document picker.
        Exported PDF files are not encrypted by this app and can be shared, copied, or
        backed up by the user or other software. Share only with recipients you trust.
        This prototype does not automatically share records with clinics or advertisers.

        5. HIPAA AND OTHER PRIVACY RULES
        HIPAA governs certain covered entities and business associates; its application
        to a standalone personal health app depends on the organization's role and
        relationships. Vital Path is being designed with HIPAA privacy and security
        requirements in mind where applicable, but HIPAA compliance has not been
        verified or established. Other privacy and consumer protection rules may apply.
        This notice is not a HIPAA authorization or an official Notice of Privacy
        Practices from a healthcare provider.

        6. GOOGLE HEALTH CONNECT — FUTURE INTEGRATION
        Vital Path plans to support Google Health Connect. No Health Connect data is
        accessed by this prototype. Before any import, you will receive information
        about the specific data requested and be asked for separate permissions through
        Health Connect. You can manage or revoke those permissions in Android settings.
        Acknowledging this notice does not grant Health Connect access.

        7. CONTROL, DELETION & RETENTION
        Entries persist locally until changed, removed through available app controls,
        or the app's stored data is cleared or uninstalled (subject to device behavior).
        This build does not yet offer complete in-app deletion or data portability tools.
        Do not rely on it as your only copy of important medical information.

        8. SECURITY STATUS & LIMITATIONS
        This prototype lacks production-grade authentication, encryption, access
        controls, account separation, audited data handling, and recovery procedures.
        It is suitable for synthetic test data only.

        9. USER ACKNOWLEDGMENT
        Selecting the acknowledgment checkbox confirms that you have read this notice
        and understand the prototype's limitations. It does not waive privacy rights,
        establish HIPAA compliance, or authorize sharing with healthcare providers.

        Before real-world deployment, the team must complete a reviewed privacy policy,
        legal and security assessment, contact and complaint procedures, and product
        terms that match the actual services and third-party integrations.
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
