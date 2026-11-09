
package com.example.vitalpath

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : ComponentActivity() {

    // DRAFT FOR TEAM REVIEW:
    // Update disclosures and security practices before public deployment.
    private val notice = """
        VITAL PATH — PRIVACY, HEALTH DATA & TERMS NOTICE
        Version: Prototype draft 1.0 | October 2026

        1. PURPOSE
        Vital Path helps individuals manually track symptoms, vital signs,
        medications, and daily health observations. It provides summaries
        that individuals may choose to bring to healthcare appointments.
        It does not diagnose, prescribe, monitor emergencies, or replace
        professional medical care.

        2. INFORMATION ENTERED
        The prototype may store a profile name and email; health conditions
        and symptoms selected by the user; daily logs, blood pressure,
        heart rate, blood glucose, sleep, medications, and notes; and the
        date the privacy notice was acknowledged.
        Some planned features are not yet connected or implemented.

        3. WHERE YOUR DATA GOES — CURRENT PROTOTYPE
        Vital Path uses Firebase Authentication to create and manage
        user accounts. Email addresses are sent to Firebase for
        authentication, while passwords are managed by Firebase
        Authentication rather than stored in Vital Path's local preferences.

        Manual health entries and medications are currently stored locally
        in the application's private storage. Cloud Firestore integration
        is under development and health records are not yet automatically
        uploaded to the database.

        Do not use real patient information in this development build.

        4. SHARING & EXPORTS
        Users may export PDF reports using Android's document picker.
        Exported PDFs are not encrypted by Vital Path and may be copied,
        shared, or backed up by other software.
        Share reports only with trusted recipients.

        5. HIPAA AND OTHER PRIVACY RULES
        HIPAA governs certain covered entities and business associates.
        Its application to a standalone personal health app depends on
        the organization's role and relationships.

        Vital Path is being designed with applicable privacy and security
        requirements in mind, but HIPAA compliance has not been verified
        or established.

        This notice is not a HIPAA authorization or an official Notice
        of Privacy Practices from a healthcare provider.

        6. GOOGLE HEALTH CONNECT
        Vital Path includes optional Google Health Connect functionality
        to import supported heart-rate measurements.

        Health Connect access requires separate user permission.
        Users can manage or revoke permissions through Android settings.
        Acknowledging this notice does not grant Health Connect access.

        7. CONTROL, DELETION & RETENTION
        Locally stored entries persist until removed through available
        controls or until the application's data is cleared or uninstalled,
        subject to device behavior.

        Firebase Authentication account data is managed separately.
        This prototype does not yet provide complete account deletion,
        cloud data deletion, or data portability features.

        8. SECURITY STATUS & LIMITATIONS
        Firebase Authentication manages account credentials, but
        Vital Path has not completed all production-grade security,
        encryption, access control, audit, and recovery requirements.

        This version is intended for development and synthetic test data.

        9. USER ACKNOWLEDGMENT
        By selecting the acknowledgment checkbox, users confirm that
        they have read this notice and understand the application's
        current limitations.

        Acknowledgment does not waive privacy rights, establish HIPAA
        compliance, or authorize sharing with healthcare providers.

        Before public deployment, the team must complete legal and
        security reviews, contact procedures, retention policies,
        and terms matching the implemented services.
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        findViewById<Button>(R.id.btnReadPrivacy).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Privacy & Health Data Notice")
                .setMessage(notice)
                .setPositiveButton("Close", null)
                .show()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnContinue).setOnClickListener {


            val nameField = findViewById<EditText>(R.id.etName)
            val emailField = findViewById<EditText>(R.id.etEmail)
            val passwordField = findViewById<EditText>(R.id.etPassword)
            val check = findViewById<CheckBox>(R.id.cbPrivacyAck)

            val name = nameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()

            when {
                name.isEmpty() -> {
                    nameField.error = "Enter a name"
                }

                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    emailField.error = "Enter a valid email"
                }

                password.length < 6 -> {
                    passwordField.error = "Password must be at least 6 characters"
                }

                !check.isChecked -> {
                    AlertDialog.Builder(this)
                        .setMessage("Please read and acknowledge the privacy notice.")
                        .setPositiveButton("OK", null)
                        .show()
                }

                else -> {
                    val continueButton = findViewById<Button>(R.id.btnContinue)
                    continueButton.isEnabled = false

                    FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->

                            continueButton.isEnabled = true

                            if (task.isSuccessful) {

                                // Store basic profile information locally.
                                // Never store the user's password here.
                                getSharedPreferences(
                                    "vitalpath_profile",
                                    MODE_PRIVATE
                                ).edit()
                                    .putString("name", name)
                                    .putString("email", email)
                                    .putLong(
                                        "noticeAcknowledgedAt",
                                        System.currentTimeMillis()
                                    )
                                    .apply()

                                Toast.makeText(
                                    this,
                                    "Account created successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                AlertDialog.Builder(this)
                                    .setTitle("Health Connect Integration")
                                    .setMessage(
                                        "Vital Path supports optional Google Health Connect " +
                                                "integration. Health-data permissions are requested " +
                                                "separately when you choose to connect."
                                    )
                                    .setPositiveButton("Continue") { _, _ ->
                                        startActivity(
                                            Intent(
                                                this,
                                                ConditionsActivity::class.java
                                            )
                                        )
                                    }
                                    .show()

                            } else {

                                Toast.makeText(
                                    this,
                                    "Registration failed: ${task.exception?.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
            }
        }
    }
}
