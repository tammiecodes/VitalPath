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
}
