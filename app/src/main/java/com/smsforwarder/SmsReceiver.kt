package com.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = PrefsManager(context)
        if (!prefs.isEnabled) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in messages) {
            // 短号码（106/955 等）在部分设备上 originatingAddress 为 null，
            // 使用 displayOriginatingAddress 作为兜底
            val rawSender = msg.originatingAddress
            val displaySender = msg.displayOriginatingAddress
            val sender = when {
                !rawSender.isNullOrBlank() -> rawSender
                !displaySender.isNullOrBlank() -> displaySender
                else -> "未知"
            }
            val body = msg.messageBody ?: ""

            // 过滤黑名单/白名单
            if (prefs.filterMode == "whitelist" && !prefs.filterList.contains(sender)) continue
            if (prefs.filterMode == "blacklist" && prefs.filterList.contains(sender)) continue

            val entry = ForwardEntry(
                sender = sender,
                body = body,
                timestamp = System.currentTimeMillis()
            )

            // 启动转发
            val forwardIntent = Intent(context, ForwardService::class.java).apply {
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
}