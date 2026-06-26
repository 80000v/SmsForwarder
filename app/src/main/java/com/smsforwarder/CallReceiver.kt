package com.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val KEY_LAST_RINGING_NUMBER = "call_last_ringing_number"
        private const val KEY_LAST_RINGING_TIME = "call_last_ringing_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val prefs = PrefsManager(context)
        if (!prefs.isEnabled) return
        if (!prefs.callForwardEnabled) return

        val sp = context.getSharedPreferences("sms_forwarder", Context.MODE_PRIVATE)
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                if (incomingNumber.isNullOrBlank()) return

                // 保存来电号码，用于后续判断是否漏接
                sp.edit()
                    .putString(KEY_LAST_RINGING_NUMBER, incomingNumber)
                    .putLong(KEY_LAST_RINGING_TIME, System.currentTimeMillis())
                    .apply()

                // 转发来电通知
                forward(context, prefs, incomingNumber, "call")
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // 检查是否有未处理的来电号码（RINGING 后直接 IDLE，未经过 OFFHOOK，即为未接来电）
                val lastRingingNumber = sp.getString(KEY_LAST_RINGING_NUMBER, null)
                if (!lastRingingNumber.isNullOrBlank()) {
                    sp.edit()
                        .remove(KEY_LAST_RINGING_NUMBER)
                        .remove(KEY_LAST_RINGING_TIME)
                        .apply()
                    // 转发未接来电
                    forward(context, prefs, lastRingingNumber, "missed_call")
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // 电话已接听，清除待处理的来电记录
                sp.edit()
                    .remove(KEY_LAST_RINGING_NUMBER)
                    .remove(KEY_LAST_RINGING_TIME)
                    .apply()
            }
        }
    }

    private fun forward(
        context: Context,
        prefs: PrefsManager,
        number: String,
        callType: String
    ) {
        val entry = ForwardEntry(
            type = callType,
            sender = number,
            body = "",
            timestamp = System.currentTimeMillis()
        )

        val forwardIntent = Intent(context, ForwardService::class.java).apply {
            putExtra("type", callType)
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
