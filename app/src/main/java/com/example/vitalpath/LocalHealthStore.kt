package com.example.vitalpath

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Demo-only local JSON storage. Not encrypted and not suitable for real patient records. */
class LocalHealthStore(private val context: Context) {
    private val file: File get() = File(context.filesDir, "vital_path_demo.json")

    @Synchronized private fun load(): JSONObject = try {
        JSONObject(file.readText())
    } catch (_: Exception) {
        JSONObject().put("checkins", JSONArray()).put("medications", JSONArray())
    }

    @Synchronized private fun save(data: JSONObject) {
        file.writeText(data.toString())
    }

    @Synchronized fun checkIns(): List<JSONObject> {
        val arr = load().optJSONArray("checkins") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            .sortedByDescending { it.optString("date") }
    }

    @Synchronized fun checkIn(date: String): JSONObject? =
        checkIns().firstOrNull { it.optString("date") == date }

    @Synchronized fun putCheckIn(entry: JSONObject) {
        val data = load()
        val all = data.optJSONArray("checkins") ?: JSONArray()
        val next = JSONArray()
        for (i in 0 until all.length()) {
            val old = all.optJSONObject(i) ?: continue
            if (old.optString("date") != entry.optString("date")) next.put(old)
        }
        next.put(entry)
        data.put("checkins", next)
        save(data)
    }

    @Synchronized fun medications(): List<JSONObject> {
        val arr = load().optJSONArray("medications") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    @Synchronized fun addMedication(name: String, dose: String, schedule: String) {
        val data = load()
        val arr = data.optJSONArray("medications") ?: JSONArray()
        arr.put(JSONObject().put("id", System.currentTimeMillis().toString())
            .put("name", name).put("dose", dose).put("schedule", schedule))
        data.put("medications", arr)
        save(data)
    }

    @Synchronized fun removeMedication(id: String) {
        val data = load()
        val arr = data.optJSONArray("medications") ?: JSONArray()
        val next = JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            if (item.optString("id") != id) next.put(item)
        }
        data.put("medications", next)
        save(data)
    }


    /** First scheduled time supplied by user; no inferred schedule for old records. */
    @Synchronized fun setMedicationSchedule(id: String, nextDueMs: Long, count: Int, period: String) {
        val data = load()
        val arr = data.optJSONArray("medications") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val med = arr.optJSONObject(i) ?: continue
            if (med.optString("id") == id) {
                med.put("nextDueMs", nextDueMs)
                med.put("frequencyCount", count)
                med.put("frequencyPeriod", period)
            }
        }
        data.put("medications", arr)
        save(data)
    }

    /** Saves one explicit confirmation (taken or missed), then advances the estimated due time. */
    @Synchronized fun recordDose(id: String, status: String): Boolean {
        if (status !in listOf("taken", "missed")) return false
        val data = load()
        val arr = data.optJSONArray("medications") ?: JSONArray()
        val med = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            .firstOrNull { it.optString("id") == id } ?: return false
        val due = med.optLong("nextDueMs", 0L)
        if (due <= 0L) return false
        val event = JSONObject().put("medId", id)
            .put("medName", med.optString("name"))
            .put("status", status).put("scheduledMs", due)
            .put("recordedMs", System.currentTimeMillis())
        val events = data.optJSONArray("doseEvents") ?: JSONArray()
        events.put(event)
        data.put("doseEvents", events)
        val n = med.optInt("frequencyCount", 1).coerceIn(1, 4)
        val period = med.optString("frequencyPeriod", "day")
        // Simplified estimated intervals; NOT a verified prescription schedule.
        val interval = when (period) {
            "week" -> 7L * 24L * 60L * 60L * 1000L / n
            "month" -> 30L * 24L * 60L * 60L * 1000L / n
            else -> 24L * 60L * 60L * 1000L / n
        }
        med.put("nextDueMs", due + interval)
        med.put("lastStatus", status)
        med.put("lastRecordedMs", System.currentTimeMillis())
        data.put("medications", arr)
        save(data)
        return true
    }

    @Synchronized fun doseEvents(): List<JSONObject> {
        val arr = load().optJSONArray("doseEvents") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }
}
