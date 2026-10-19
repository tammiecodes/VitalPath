package com.example.vitalpath

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class CheckInActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)

        // Display selected calendar date, if provided.
        val date = intent.getStringExtra("selectedDate")
        if (date != null) {
            findViewById<TextView>(R.id.txtCheckInDate).text =
                "Check-In for $date"
        }

        findViewById<Button>(R.id.btnSaveCheckIn)
            .setOnClickListener {
                Toast.makeText(
                    this,
                    "Saving check-ins coming soon!",
                    Toast.LENGTH_SHORT
                ).show()
            }

        findViewById<Button>(R.id.btnBackCheckIn)
            .setOnClickListener {
                finish()
            }
    }
}
