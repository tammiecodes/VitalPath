package com.example.vitalpath

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FitbitActivity : ComponentActivity() {

    private lateinit var statusText: TextView
    private lateinit var heartText: TextView
    private lateinit var timeText: TextView
    private lateinit var syncButton: Button

    // True when authorization was requested by the Sync button.
    private var syncAfterAuthorization = false

    private val healthScope = Scope(
        "https://www.googleapis.com/auth/" +
                "googlehealth.health_metrics_and_measurements.readonly"
    )

    private val authorizationLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val authorizationResult =
                        Identity.getAuthorizationClient(this)
                            .getAuthorizationResultFromIntent(result.data)

                    handleAuthorization(authorizationResult)

                } catch (e: Exception) {
                    syncButton.isEnabled = true
                    statusText.text =
                        "Authorization failed: ${e.localizedMessage}"
                }
            } else {
                syncButton.isEnabled = true
                statusText.text = "Google authorization canceled"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fitbit)

        statusText = findViewById(R.id.txtFitbitStatus)
        heartText = findViewById(R.id.txtFitbitHeartRate)
        timeText = findViewById(R.id.txtFitbitHeartTime)
        syncButton = findViewById(R.id.btnSyncFitbit)

        findViewById<Button>(R.id.btnAuthorizeFitbit)
            .setOnClickListener {
                requestHealthAccess(false)
            }

        syncButton.setOnClickListener {
            requestHealthAccess(true)
        }

        findViewById<Button>(R.id.btnBackFitbit)
            .setOnClickListener {
                finish()
            }
    }

    private fun requestHealthAccess(sync: Boolean) {

        syncAfterAuthorization = sync
        syncButton.isEnabled = false

        statusText.text = if (sync) {
            "Connecting to Google Health..."
        } else {
            "Requesting Google Health permission..."
        }

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(healthScope))
            .build()

        Identity.getAuthorizationClient(this)
            .authorize(request)
            .addOnSuccessListener { result ->

                if (result.hasResolution()) {
                    val pendingIntent = result.pendingIntent

                    if (pendingIntent != null) {
                        try {
                            authorizationLauncher.launch(
                                IntentSenderRequest.Builder(
                                    pendingIntent.intentSender
                                ).build()
                            )
                        } catch (e: Exception) {
                            syncButton.isEnabled = true
                            statusText.text =
                                "Unable to open Google authorization"
                        }
                    } else {
                        syncButton.isEnabled = true
                        statusText.text =
                            "Authorization screen unavailable"
                    }
                } else {
                    handleAuthorization(result)
                }
            }
            .addOnFailureListener { error ->
                syncButton.isEnabled = true
                statusText.text =
                    "Google connection failed: ${error.localizedMessage}"
            }
    }

    private fun handleAuthorization(result: AuthorizationResult) {

        val granted = result.grantedScopes.contains(
            healthScope.scopeUri
        )

        val token = result.accessToken

        if (!granted || token.isNullOrBlank()) {
            syncButton.isEnabled = true
            statusText.text =
                "Heart-rate permission was not granted"
            return
        }

        if (syncAfterAuthorization) {
            fetchFitbitHeartRate(token)
        } else {
            syncButton.isEnabled = true
            statusText.text =
                "Connected! Tap Sync Latest Heart Rate."
        }
    }

    private fun fetchFitbitHeartRate(token: String) {

        statusText.text = "Loading Fitbit measurements..."

        // Network requests must not run on the UI thread.
        Thread {
            var connection: HttpURLConnection? = null

            try {
                val endpoint =
                    "https://health.googleapis.com/v4/" +
                            "users/me/dataTypes/heart-rate/dataPoints" +
                            "?pageSize=100"

                connection = URL(endpoint)
                    .openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val status = connection.responseCode

                if (status != 200) {
                    val error = connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: "No additional details"

                    throw Exception(
                        "HTTP $status: ${error.take(200)}"
                    )
                }

                val body = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val points = JSONObject(body)
                    .optJSONArray("dataPoints")

                var latestTime: Instant? = null
                var latestBpm: String? = null
                var latestDevice = "Fitbit"

                if (points != null) {
                    for (i in 0 until points.length()) {

                        val item = points.optJSONObject(i)
                            ?: continue

                        val source = item.optJSONObject(
                            "dataSource"
                        ) ?: continue

                        val device = source
                            .optJSONObject("device")
                            ?.optString("displayName")
                            .orEmpty()

                        val platform = source.optString(
                            "platform"
                        )

                        // Use actual Versa 2 measurements.
                        if (!platform.equals(
                                "FITBIT", ignoreCase = true
                            ) || !device.contains(
                                "Versa 2", ignoreCase = true
                            )
                        ) {
                            continue
                        }

                        val reading = item.optJSONObject(
                            "heartRate"
                        ) ?: continue

                        val bpm = reading.optString(
                            "beatsPerMinute"
                        )

                        val timestamp = reading
                            .optJSONObject("sampleTime")
                            ?.optString("physicalTime")
                            .orEmpty()

                        if (bpm.isBlank() ||
                            timestamp.isBlank()
                        ) {
                            continue
                        }

                        val measuredAt = Instant.parse(
                            timestamp
                        )

                        val previous = latestTime

                        if (previous == null ||
                            measuredAt.isAfter(previous)
                        ) {
                            latestTime = measuredAt
                            latestBpm = bpm
                            latestDevice = device
                        }
                    }
                }

                val bpm = latestBpm
                val measuredAt = latestTime

                runOnUiThread {

                    syncButton.isEnabled = true

                    if (bpm == null || measuredAt == null) {
                        statusText.text =
                            "No Versa 2 readings found in " +
                                    "the first 100 returned records."

                        heartText.text =
                            "Heart Rate: No reading"
                        timeText.text =
                            "Try syncing the Fitbit on your iPhone."
                    } else {

                        val localTime = measuredAt
                            .atZone(ZoneId.systemDefault())
                            .format(
                                DateTimeFormatter.ofPattern(
                                    "MMM d, yyyy h:mm a"
                                )
                            )

                        heartText.text = "$bpm BPM"
                        timeText.text =
                            "$latestDevice · Measured $localTime"

                        statusText.text =
                            "Fitbit heart rate retrieved!"
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {
                    syncButton.isEnabled = true
                    statusText.text =
                        "Sync failed: ${e.localizedMessage}"
                }

            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
