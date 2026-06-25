package com.smsforwarder

import android.content.Context

class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("sms_forwarder", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(v) = prefs.edit().putBoolean("enabled", v).apply()

    var method: String
        get() = prefs.getString("method", "webhook") ?: "webhook"
        set(v) = prefs.edit().putString("method", v).apply()

    var webhookUrl: String
        get() = prefs.getString("webhook_url", "") ?: ""
        set(v) = prefs.edit().putString("webhook_url", v).apply()

    var ntfyTopic: String
        get() = prefs.getString("ntfy_topic", "") ?: ""
        set(v) = prefs.edit().putString("ntfy_topic", v).apply()

    var ntfyServer: String
        get() = prefs.getString("ntfy_server", "https://ntfy.sh") ?: "https://ntfy.sh"
        set(v) = prefs.edit().putString("ntfy_server", v).apply()

    var filterMode: String
        get() = prefs.getString("filter_mode", "none") ?: "none"
        set(v) = prefs.edit().putString("filter_mode", v).apply()

    var filterList: Set<String>
        get() = prefs.getStringSet("filter_list", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("filter_list", v).apply()

    fun saveHistory(entry: ForwardEntry) {
        val history = prefs.getString("history", null)
        val list = if (history != null) history.split("|||").toMutableList() else mutableListOf()
        list.add(0, "${entry.timestamp}|${entry.sender}|${entry.body}")
        if (list.size > 100) list.removeAt(list.lastIndex)
        prefs.edit().putString("history", list.joinToString("|||")).apply()
    }

    fun getHistory(): List<ForwardEntry> {
        val history = prefs.getString("history", null) ?: return emptyList()
        return history.split("|||").mapNotNull { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size == 3) {
                ForwardEntry(
                    timestamp = parts[0].toLongOrNull() ?: 0L,
                    sender = parts[1],
                    body = parts[2]
                )
            } else null
        }
    }
}

data class ForwardEntry(
    val sender: String = "",
    val body: String = "",
    val timestamp: Long = 0L
)