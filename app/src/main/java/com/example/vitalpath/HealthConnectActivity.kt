package com.example.vitalpath

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HealthConnectActivity : ComponentActivity() {

    private lateinit var statusText: TextView
    private lateinit var heartText: TextView
    private lateinit var timeText: TextView

    private val permission = HealthPermission.getReadPermission(
        HeartRateRecord::class
    )

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (permission in granted) {
            readHeartRate()
        } else {
            statusText.text = "Heart-rate permission not granted"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_connect)

        statusText = findViewById(R.id.txtHealthStatus)
        heartText = findViewById(R.id.txtHeartRate)
        timeText = findViewById(R.id.txtHeartTime)

        // Show any previously imported reading.
        val prefs = getSharedPreferences(
            "vitalpath_health_connect",
            MODE_PRIVATE
        )

        if (prefs.contains("heart_rate")) {
            heartText.text =
                "Heart Rate: ${prefs.getLong("heart_rate", 0)} BPM"
            timeText.text =
                "Last imported reading: " +
                        prefs.getString("heart_time", "")
        }

        findViewById<Button>(R.id.btnConnectHealth)
            .setOnClickListener {
                connectHealth()
            }

        findViewById<Button>(R.id.btnRefreshHealth)
            .setOnClickListener {
                connectHealth()
            }

        findViewById<Button>(R.id.btnBackHealth)
            .setOnClickListener {
                finish()
            }
    }

    private fun connectHealth() {

        val sdkStatus = HealthConnectClient.getSdkStatus(this)

        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            statusText.text =
                "Health Connect is unavailable. " +
                        "Check your Android version and Health Connect setup."
            return
        }

        lifecycleScope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(
                    this@HealthConnectActivity
                )

                val granted = client.permissionController
                    .getGrantedPermissions()

                if (permission in granted) {
                    readHeartRate()
                } else {
                    statusText.text = "Requesting permission..."
                    permissionLauncher.launch(setOf(permission))
                }

            } catch (e: Exception) {
                statusText.text =
                    "Connection error: ${e.message}"
            }
        }
    }

    private fun readHeartRate() {

        lifecycleScope.launch {
            try {
                statusText.text = "Reading heart-rate data..."

                val client = HealthConnectClient.getOrCreate(
                    this@HealthConnectActivity
                )

                val granted = client.permissionController
                    .getGrantedPermissions()

                if (permission !in granted) {
                    statusText.text = "Permission required"
                    return@launch
                }

                val end = Instant.now()
                val start = end.minus(24, ChronoUnit.HOURS)

                var pageToken: String? = null
                var latestTime: Instant? = null
                var latestBpm: Long? = null

                // Read all pages within the requested 24 hours.
                do {
                    val response = client.readRecords(
                        ReadRecordsRequest(
                            recordType = HeartRateRecord::class,
                            timeRangeFilter =
                                TimeRangeFilter.between(start, end),
                            pageToken = pageToken
                        )
                    )

                    response.records.forEach { record ->
                        record.samples.forEach { sample ->
                            val previous = latestTime
                            if (previous == null || sample.time.isAfter(previous)) {
                                latestTime = sample.time
                                latestBpm = sample.beatsPerMinute
                            }

                        }
                    }

                    pageToken = response.pageToken

                } while (pageToken != null)

                val bpm = latestBpm
                val timestamp = latestTime

                if (bpm == null || timestamp == null) {
                    statusText.text = "Connected: permission granted"
                    heartText.text = "Heart Rate: No data"
                    timeText.text =
                        "No heart-rate readings in the past 24 hours"
                    return@launch
                }

                val formattedTime = timestamp
                    .atZone(ZoneId.systemDefault())
                    .format(
                        DateTimeFormatter.ofPattern(
                            "MMM d, yyyy h:mm a"
                        )
                    )

                heartText.text = "Heart Rate: $bpm BPM"
                timeText.text = "Measured: $formattedTime"
                statusText.text = "Heart-rate import successful"

                // Save the imported reading separately from
                // manually entered daily check-ins.
                getSharedPreferences(
                    "vitalpath_health_connect",
                    MODE_PRIVATE
                ).edit()
                    .putLong("heart_rate", bpm)
                    .putString("heart_time", formattedTime)
                    .putString(
                        "heart_timestamp",
                        timestamp.toString()
                    )
                    .apply()

            } catch (e: Exception) {
                statusText.text =
                    "Unable to read heart rate: ${e.message}"
            }
        }
    }
}
