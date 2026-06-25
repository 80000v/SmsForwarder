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
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("短信转发运行中")
            .setContentText("正在监听短信…")
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

        startForeground(1, notification)

        intent?.let {
            val sender = it.getStringExtra("sender") ?: return@let
            val body = it.getStringExtra("body") ?: return@let
            val timestamp = it.getLongExtra("timestamp", 0L)
            val entry = ForwardEntry(sender, body, timestamp)

            val prefs = PrefsManager(this)

            Forwarder.send(
                method = prefs.method,
                webhookUrl = prefs.webhookUrl,
                ntfyTopic = prefs.ntfyTopic,
                ntfyServer = prefs.ntfyServer,
                entry = entry
            ) { success, message ->
                prefs.saveHistory(entry.copy(body = body.take(100)))
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