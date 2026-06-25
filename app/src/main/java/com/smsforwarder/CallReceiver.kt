package com.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val prefs = PrefsManager(context)
        if (!prefs.isEnabled) return
        if (!prefs.callForwardEnabled) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            ?: return

        val entry = ForwardEntry(
            type = "call",
            sender = incomingNumber,
            body = "",
            timestamp = System.currentTimeMillis()
        )

        val forwardIntent = Intent(context, ForwardService::class.java).apply {
            putExtra("type", "call")
            putExtra("sender", entry.sender)
            putExtra("body", entry.body)
            putExtra("timestamp", entry.timestamp)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(forwardIntent)
            } else {
                context.startService(forwardIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
