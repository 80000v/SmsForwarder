package com.smsforwarder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForwardService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = intent?.getStringExtra("type") ?: "sms"
        val title = when (type) {
            "call" -> "来电转发运行中"
            "missed_call" -> "未接来电转发运行中"
            else -> "短信转发运行中"
        }
        val text = when (type) {
            "call", "missed_call" -> "正在监听来电…"
            else -> "正在监听短信…"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .build()

        try {
            startForeground(1, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }

        intent?.let {
            try {
                val sender = it.getStringExtra("sender") ?: return@let
                val body = it.getStringExtra("body") ?: return@let
                val timestamp = it.getLongExtra("timestamp", 0L)
                val entry = ForwardEntry(sender, body, timestamp, type)

                val prefs = PrefsManager(this)
                prefs.saveHistory(entry.copy(body = body.take(100)))

                Forwarder.send(
                    method = prefs.method,
                    webhookUrl = prefs.webhookUrl,
                    pushplusToken = prefs.pushplusToken,
                    entry = entry
                ) { _, _ -> }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "短信转发",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "短信转发服务运行状态"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "sms_forwarder_channel"
    }
}