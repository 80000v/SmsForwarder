package com.smsforwarder

import android.content.Context

class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("sms_forwarder", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(v) = prefs.edit().putBoolean("enabled", v).apply()

    var method: String
        get() = prefs.getString("method", "pushplus") ?: "pushplus"
        set(v) = prefs.edit().putString("method", v).apply()

    var webhookUrl: String
        get() = prefs.getString("webhook_url", "") ?: ""
        set(v) = prefs.edit().putString("webhook_url", v).apply()

    var pushplusToken: String
        get() = prefs.getString("pushplus_token", "") ?: ""
        set(v) = prefs.edit().putString("pushplus_token", v).apply()

    var filterMode: String
        get() = prefs.getString("filter_mode", "none") ?: "none"
        set(v) = prefs.edit().putString("filter_mode", v).apply()

    var filterList: Set<String>
        get() = prefs.getStringSet("filter_list", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("filter_list", v).apply()

    var callForwardEnabled: Boolean
        get() = prefs.getBoolean("call_forward_enabled", true)
        set(v) = prefs.edit().putBoolean("call_forward_enabled", v).apply()

    fun saveHistory(entry: ForwardEntry) {
        val history = prefs.getString("history", null)
        val list = if (history != null) history.split("|||").toMutableList() else mutableListOf()
        list.add(0, "${entry.timestamp}|${entry.sender}|${entry.body}|${entry.type}")
        if (list.size > 100) list.removeAt(list.lastIndex)
        prefs.edit().putString("history", list.joinToString("|||")).apply()
    }

    fun getHistory(): List<ForwardEntry> {
        val history = prefs.getString("history", null) ?: return emptyList()
        return history.split("|||").mapNotNull { line ->
            val parts = line.split("|", limit = 4)
            if (parts.size >= 3) {
                ForwardEntry(
                    timestamp = parts[0].toLongOrNull() ?: 0L,
                    sender = parts[1],
                    body = parts[2],
                    type = if (parts.size >= 4) parts[3] else "sms"
                )
            } else null
        }
    }
}

data class ForwardEntry(
    val sender: String = "",
    val body: String = "",
    val timestamp: Long = 0L,
    val type: String = "sms"
)