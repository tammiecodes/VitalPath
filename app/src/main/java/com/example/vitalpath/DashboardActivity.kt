package com.example.vitalpath

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class DashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        findViewById<Button>(R.id.btnDailyCheckIn)
            .setOnClickListener {
                startActivity(
                    Intent(this, CheckInActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnCalendar)
            .setOnClickListener {
                startActivity(
                    Intent(this, CalendarActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnMedicationsPage)
            .setOnClickListener {
                startActivity(
                    Intent(this, MedicationsActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnHealthSummary)
            .setOnClickListener {
                startActivity(
                    Intent(this, SummaryActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnDoctorReport)
            .setOnClickListener {
                startActivity(
                    Intent(this, ReportActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnProfile)
            .setOnClickListener {
                startActivity(
                    Intent(this, ProfileActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnHealthConnect)
            .setOnClickListener {
                startActivity(
                    Intent(this, HealthConnectActivity::class.java)
                )
            }


        findViewById<Button>(R.id.btnLogout)
            .setOnClickListener {
                val intent = Intent(
                    this,
                    MainActivity::class.java
                )

                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
            }
    }
}
