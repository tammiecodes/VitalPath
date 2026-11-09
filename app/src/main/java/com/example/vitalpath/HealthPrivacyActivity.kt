package com.example.vitalpath

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

class HealthPrivacyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFF101827.toInt())
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
        }

        fun addText(message: String, size: Float) {
            layout.addView(TextView(this).apply {
                text = message
                textSize = size
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 12, 0, 12)
            })
        }

        addText("Vital Path — Health Data Privacy", 24f)

        addText(
            "Vital Path requests read-only access to heart-rate " +
                    "records in Google Health Connect when you choose " +
                    "to connect your health data.",
            16f
        )

        addText(
            "The app displays the latest available reading and " +
                    "stores a copy locally on this device. It does not " +
                    "automatically transmit this reading to clinicians " +
                    "or other services.",
            16f
        )

        addText(
            "You can deny or revoke Health Connect access " +
                    "through Android settings. Revoking access prevents " +
                    "future reads but does not automatically erase " +
                    "previously imported copies.",
            16f
        )

        addText(
            "Vital Path is a development prototype. Do not use " +
                    "real patient health information until appropriate " +
                    "privacy, security, deletion, and access controls " +
                    "have been implemented and reviewed.",
            16f
        )

        scroll.addView(layout)
        setContentView(scroll)
    }
}
